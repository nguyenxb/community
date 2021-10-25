package com.nguyenxb.community;

import org.aspectj.lang.annotation.Aspect;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testString(){
        String redisKey = "test:count";
        redisTemplate.opsForValue().set(redisKey,1);

        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashes(){
        String redisKey = "test:user";

        redisTemplate.opsForHash().put(redisKey,"id",1);
        redisTemplate.opsForHash().put(redisKey,"username","zhangsan");

        redisTemplate.opsForHash().get(redisKey,"id");
        redisTemplate.opsForHash().get(redisKey,"username");
    }

    @Test
    public void testLists(){
        String redisKey = "test:ids";

        redisTemplate.opsForList().leftPush(redisKey,101);
        redisTemplate.opsForList().leftPush(redisKey,102);
        redisTemplate.opsForList().rightPush(redisKey,103);

        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey,0));
        System.out.println(redisTemplate.opsForList().range(redisKey,0,2));

        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().rightPop(redisKey));
        System.out.println(redisTemplate.opsForList().rightPop(redisKey));
    }

    @Test
    public void testSets(){
        String redisKey = "test:teacher";

        redisTemplate.opsForSet().add(redisKey,"诸葛亮","马超","赵云","张飞","关羽");

        System.out.println(redisTemplate.opsForSet().size(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().members(redisKey));
    }

    @Test
    public void testSortedSets(){
        String redisKey = "test:students";

        redisTemplate.opsForZSet().add(redisKey,"张三",10);
        redisTemplate.opsForZSet().add(redisKey,"李四",60);
        redisTemplate.opsForZSet().add(redisKey,"王五",90);
        redisTemplate.opsForZSet().add(redisKey,"赵六",70);
        redisTemplate.opsForZSet().add(redisKey,"胜七",30);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey,"张三"));
        System.out.println(redisTemplate.opsForZSet().score(redisKey,"张三"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey,"张三"));
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey,0,2));

    }

    @Test
    public void testKeys(){
        redisTemplate.delete("test:user");

        System.out.println(redisTemplate.hasKey("test:user"));

        redisTemplate.expire("test:students",10, TimeUnit.SECONDS);
    }

    // 多次访问同一个key, 使用绑定的方式
    @Test
    public void testBoundOperations(){
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);

        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());

    }

    // 编程式事务
    @Test
    public void testTransactional(){
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String redisKey = "test:tx";

                redisOperations.multi();

                redisOperations.opsForSet().add(redisKey,"zhangsan ");
                redisOperations.opsForSet().add(redisKey,"lisi");
                redisOperations.opsForSet().add(redisKey,"wangwu");

                //  在事务期间查询无效
                System.out.println(redisOperations.opsForSet().members(redisKey));
                return redisOperations.exec();



            }
        });
        System.out.println(obj);
    }

    // 统计20 w个重复数据的独立总数, 使用 HyperLogLog 类型
    @Test
    public void testHyperLogLog(){
        String redisKey = "test:hll:01";

        // 生成1-100000的数据
        for (int i = 0; i < 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey,i);
        }
        // 生成 1-10w 的随机数据
        for (int i = 0; i < 100000; i++) {
            int r = (int) (Math.random() * 100000 +1);
            redisTemplate.opsForHyperLogLog().add(redisKey,i);
        }

        Long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size); // 99556
    }

    // 将三组数据合并,再统计合并后的重复数据的独立总数
    @Test
    public void testHyperLogLgoUnion(){
        String redisKey2 = "test:hll:02";
        // 生成 1-1w 的随机数据
        for (int i = 0; i < 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2,i);
        }

        String redisKey3 = "test:hll:03";
        // 生成 1-1.5w 的随机数据
        for (int i = 0; i < 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3,i);
        }

        String redisKey4 = "test:hll:04";
        // 生成 1w-2w 的随机数据
        for (int i = 10001; i < 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3,i);
        }

        // 合并三组数据
        String unionKey = "test:hll:union";
        Long union = redisTemplate.opsForHyperLogLog().union(unionKey,redisKey2, redisKey3, redisKey4);
        System.out.println(union);

        Long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size); // 19833

    }

    // bitmap
    // 统计一组数据的布尔值
    @Test
    public void testBitMap(){
        String redisKey = "test:bm:01";

        // 记录
        redisTemplate.opsForValue().setBit(redisKey,1,true);
        redisTemplate.opsForValue().setBit(redisKey,4,true);
        redisTemplate.opsForValue().setBit(redisKey,7,true);

        // 查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,2));

        // 统计是true的个数
       Object obj =  redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);

    }

    // 统计三组数据的布尔值,并对这3组数据做or运算
    @Test
    public void testBitMapOperation(){
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2,0,true);
        redisTemplate.opsForValue().setBit(redisKey2,1,true);
        redisTemplate.opsForValue().setBit(redisKey2,2,true);

        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey2,2,true);
        redisTemplate.opsForValue().setBit(redisKey2,3,true);
        redisTemplate.opsForValue().setBit(redisKey2,4,true);

         String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey2,4,true);
        redisTemplate.opsForValue().setBit(redisKey2,5,true);
        redisTemplate.opsForValue().setBit(redisKey2,6,true);

        // 进行or运算
        String redisKey = "test:bm:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });

        System.out.println(obj);

        System.out.println(redisTemplate.opsForValue().getBit(redisKey,0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,5));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,6));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,7)); // 这个位置的数据不存在, 默认是false


    }

}
