import requests
import time
import threading
import math
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import List

# ============================================================
# 配置
# ============================================================
URL = "https://api.xiaomimimo.com/v1/chat/completions"
API_KEY = "sk-caqpq4v577d1znoil0wjxyw63elrsa93qvfiubv3s8bv27h1"
MODEL = "mimo-v2.5-pro"
MAX_CONCURRENCY = 300
CLIENT_TIMEOUT = 65        # 客户端超时 (秒)，略高于服务端 60s 网关超时
MAX_RETRIES = 1            # 请求级重试次数（最多 1 次重试，避免长时间卡住）
RETRY_BACKOFF = 2          # 重试间隔 (秒)

# ============================================================
# 单次请求 (带重试)
# ============================================================
PAYLOAD = {
    "model": MODEL,
    "messages": [{"role": "user", "content": "用中文简单介绍一下深度学习。"}],
    "temperature": 0.7,
    "max_tokens": 256,
    "stream": False,
}

def _build_session() -> requests.Session:
    s = requests.Session()
    s.headers.update({
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
    })
    return s

def single_request(session: requests.Session) -> dict:
    """发送一次请求 (含重试)，返回详细结果。

    返回值 dict 必有 "status" 字段:
        "ok"       — 成功（可能经过重试）
        "fail"     — 最终失败
    成功时额外字段: latency, final_latency, completion_tokens, retried
    失败时额外字段: reason (最后一次失败的原因), attempts (每次尝试的结果摘要)
    """
    total_latency = 0.0
    final_latency = 0.0
    attempt_log = []

    for attempt in range(1 + MAX_RETRIES):
        try:
            start = time.perf_counter()
            resp = session.post(URL, json=PAYLOAD, timeout=CLIENT_TIMEOUT)
            this_latency = time.perf_counter() - start
            total_latency += this_latency

            if resp.status_code == 200:
                final_latency = this_latency
                data = resp.json()
                u = data.get("usage", {})
                return {
                    "status": "ok",
                    "latency": total_latency,
                    "final_latency": final_latency,
                    "completion_tokens": u.get("completion_tokens", 0),
                    "retried": attempt > 0,
                    "attempt_log": attempt_log,
                }
            # 5xx 服务端错误 → 等待后重试
            if resp.status_code >= 500:
                attempt_log.append(f"HTTP {resp.status_code}")
                if attempt < MAX_RETRIES:
                    time.sleep(RETRY_BACKOFF * (attempt + 1))
                    total_latency += RETRY_BACKOFF * (attempt + 1)
                    session = _build_session()
                    continue
                return {"status": "fail", "reason": f"HTTP {resp.status_code}", "attempts": attempt_log}
            # 非 5xx（如 4xx），不重试
            return {"status": "fail", "reason": f"HTTP {resp.status_code}", "attempts": attempt_log}

        except requests.Timeout:
            total_latency += CLIENT_TIMEOUT
            attempt_log.append("client_timeout")
            if attempt < MAX_RETRIES:
                time.sleep(RETRY_BACKOFF)
                total_latency += RETRY_BACKOFF
                session = _build_session()
                continue
            return {"status": "fail", "reason": "client_timeout", "attempts": attempt_log}
        except requests.ConnectionError as e:
            attempt_log.append("connection_error")
            if attempt < MAX_RETRIES:
                time.sleep(RETRY_BACKOFF)
                total_latency += RETRY_BACKOFF
                session = _build_session()
                continue
            return {"status": "fail", "reason": "connection_error", "attempts": attempt_log}

    return {"status": "fail", "reason": "unknown", "attempts": attempt_log}


# ============================================================
# 单轮并发测试
# ============================================================
@dataclass
class BenchResult:
    concurrency: int
    total_requests: int
    success: int
    success_retried: int    # 经过重试才成功的请求数
    fail: int
    avg_latency: float      # 真实延迟（含失败重试耗时）
    p50_latency: float
    p90_latency: float
    p99_latency: float
    avg_output_tps: float   # 基于 final_latency 的平均单请求输出 tok/s
    p50_output_tps: float
    p90_output_tps: float
    total_output_tokens: int
    total_time: float
    throughput_tok_s: float
    throughput_req_s: float

def percentile(sorted_list: list, p: float) -> float:
    """线性插值百分位，适配小样本。p 取值 0-100。"""
    n = len(sorted_list)
    if n == 0:
        return 0.0
    if n == 1:
        return sorted_list[0]
    rank = (p / 100.0) * (n - 1)
    lo = int(rank)
    hi = min(lo + 1, n - 1)
    frac = rank - lo
    return sorted_list[lo] * (1 - frac) + sorted_list[hi] * frac


def run_round(concurrency: int, requests_per_worker: int = 1) -> BenchResult:
    """一轮并发测试：concurrency 个线程，每个发 requests_per_worker 次请求。"""
    total = concurrency * requests_per_worker
    print(f"\n  并发={concurrency}  总请求={total}  运行中...", flush=True)

    latencies = []          # 真实延迟（含重试开销）
    final_latencies = []    # 仅最终成功尝试的延迟
    output_tps_list = []    # 基于 final_latency 计算的 tok/s
    total_output = 0
    success = 0
    success_retried = 0
    fail = 0
    fail_reasons = {}       # 失败原因统计: {reason: count}
    lock = threading.Lock()

    round_start = time.perf_counter()

    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        def worker():
            session = _build_session()
            lats = []
            final_lats = []
            tps = []
            out = 0
            s = 0
            sr = 0
            f = 0
            reasons = {}

            for _ in range(requests_per_worker):
                r = single_request(session)
                if r["status"] == "fail":
                    f += 1
                    reason = r["reason"]
                    reasons[reason] = reasons.get(reason, 0) + 1
                else:
                    s += 1
                    if r["retried"]:
                        sr += 1
                    lats.append(r["latency"])
                    final_lats.append(r["final_latency"])
                    tps.append(r["completion_tokens"] / r["final_latency"])
                    out += r["completion_tokens"]

            session.close()
            return s, sr, f, reasons, lats, final_lats, tps, out

        futures = [pool.submit(worker) for _ in range(concurrency)]

        done = 0
        for f in as_completed(futures):
            s, sr, f_cnt, reasons, lats, final_lats, tps, out = f.result()
            with lock:
                success += s
                success_retried += sr
                fail += f_cnt
                for reason, count in reasons.items():
                    fail_reasons[reason] = fail_reasons.get(reason, 0) + count
                latencies.extend(lats)
                final_latencies.extend(final_lats)
                output_tps_list.extend(tps)
                total_output += out
                done += 1
                if done % 10 == 0 or done == concurrency:
                    t_elapsed = time.perf_counter() - round_start
                    print(f"  [{done}/{concurrency}] 成功={success} 失败={fail}  已耗时={t_elapsed:.0f}s", flush=True)

    round_end = time.perf_counter()
    total_time = round_end - round_start

    latencies.sort()
    output_tps_list.sort()
    n = len(latencies)
    n_tps = len(output_tps_list)

    avg_lat = sum(latencies) / n if n > 0 else 0
    p50 = percentile(latencies, 50)
    p90 = percentile(latencies, 90)
    p99 = percentile(latencies, 99)
    avg_tps = sum(output_tps_list) / n_tps if n_tps > 0 else 0
    p50_tps = percentile(output_tps_list, 50)
    p90_tps = percentile(output_tps_list, 90)
    throughput_tok = total_output / total_time if total_time > 0 else 0
    throughput_req = success / total_time if total_time > 0 else 0

    result = BenchResult(
        concurrency=concurrency,
        total_requests=total,
        success=success,
        success_retried=success_retried,
        fail=fail,
        avg_latency=avg_lat,
        p50_latency=p50,
        p90_latency=p90,
        p99_latency=p99,
        avg_output_tps=avg_tps,
        p50_output_tps=p50_tps,
        p90_output_tps=p90_tps,
        total_output_tokens=total_output,
        total_time=total_time,
        throughput_tok_s=throughput_tok,
        throughput_req_s=throughput_req,
    )

    retry_note = f"  其中重试成功={success_retried}" if success_retried > 0 else ""
    print(f"完成  成功={success}/{total}{retry_note}  失败={fail}  平均单请求输出={avg_tps:.1f} tok/s  P50={p50_tps:.1f} tok/s  P90={p90_tps:.1f} tok/s")
    if fail_reasons:
        print(f"  失败原因:", ", ".join(f"{k}×{v}" for k, v in sorted(fail_reasons.items())))
    return result


# ============================================================
# 主循环
# ============================================================
def main():
    print("=" * 70)
    print("  LoadPup - LLM API 交互式压测")
    print(f"  Endpoint: {URL}")
    print(f"  Model:    {MODEL}")
    print(f"  最大并发: {MAX_CONCURRENCY}")
    print("=" * 70)

    results: List[BenchResult] = []

    while True:
        print("\n" + "-" * 70)
        raw = input("请输入并发数 (q=退出, l=显示汇总): ").strip()

        if raw.lower() == "q":
            break

        if raw.lower() == "l":
            if not results:
                print("  暂无测试结果")
                continue
            print("\n" + "=" * 110)
            print(f"{'并发':<6} {'成功/总数':<10} {'重试成功':<8} {'平均输出tok/s':<15} {'P50 tok/s':<12} {'P90 tok/s':<12} {'系统吞吐tok/s':<16} {'平均真实延迟(s)':<16}")
            print("-" * 110)
            for r in results:
                sys_tps = f"{r.throughput_tok_s:.1f}" if r.throughput_tok_s > 0 else "-"
                print(f"{r.concurrency:<6} {r.success}/{r.total_requests:<9} {r.success_retried:<8} {r.avg_output_tps:<15.1f} {r.p50_output_tps:<12.1f} {r.p90_output_tps:<12.1f} {sys_tps:<16} {r.avg_latency:<16.2f}")
            print("=" * 110)
            continue

        try:
            c = int(raw)
            if c < 1 or c > MAX_CONCURRENCY:
                print(f"  请输入 1-{MAX_CONCURRENCY} 之间的整数")
                continue
        except ValueError:
            print("  请输入整数、'q' 或 'l'")
            continue

        result = run_round(c)
        results.append(result)


if __name__ == "__main__":
    main()
