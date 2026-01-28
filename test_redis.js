// 使用 Node.js 连接 Redis 并检查数据
const net = require('net');

const client = new net.Socket();
const host = '59.110.22.74';
const port = 6379;
const password = 'Redis@666';

client.connect(port, host, () => {
    console.log('Connected to Redis');
    
    // 发送 AUTH 命令
    client.write(`*2\r\n$4\r\nAUTH\r\n$10\r\n${password}\r\n`);
    
    setTimeout(() => {
        // 查询日榜 ZSet
        client.write('*4\r\n$6\r\nZREVRANGE\r\n$32\r\nhot:articles:zset:day:2026-01-28\r\n$1\r\n0\r\n$2\r\n-1\r\n');
        
        setTimeout(() => {
            // 查询周榜 ZSet
            client.write('*4\r\n$6\r\nZREVRANGE\r\n$31\r\nhot:articles:zset:week:2026-W05\r\n$1\r\n0\r\n$2\r\n-1\r\n');
            
            setTimeout(() => {
                // 查询所有匹配的 key
                client.write('*2\r\n$8\r\nKEYS\r\n$20\r\nhot:articles:zset:*\r\n');
                
                setTimeout(() => {
                    client.end();
                }, 500);
            }, 500);
        }, 500);
    }, 500);
});

client.on('data', (data) => {
    console.log('Redis response:', data.toString());
});

client.on('error', (err) => {
    console.error('Redis error:', err.message);
});

client.on('close', () => {
    console.log('Connection closed');
});
