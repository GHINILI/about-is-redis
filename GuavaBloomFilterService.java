package com.lwj.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GuavaBloomFilterService{
    public static final int _1W = 10000;
    //布隆过滤器里预计要插入多少数据
    public static int size = 100 * _1W;
    //误判率,它越小误判的个数也就越少(思考，是不是可以设置的无限小，没有误判岂不更好)
    //fpp the desired false positive probability
    /**
      *fpp 0.03 占729w bit 5个哈希函数
      *fpp 0.01 占956w bit 7个哈希函数
      */
    public static double fpp = 0.03;
    // 构建布隆过滤器
    private static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), size,fpp);
    public void guavaBloomFilter(){
        //1 先往布隆过滤器里面插入100万的样本数据
        for (int i = 1; i <=size; i++) {
            bloomFilter.put(i);
        }
        //故意取10万个不在过滤器里的值，看看有多少个会被认为在过滤器里
        List<Integer> list = new ArrayList<>(10 * _1W);
        for (int i = size+1; i <= size + (10 *_1W); i++) {
            if (bloomFilter.mightContain(i)) {
                log.info("被误判了:{}",i);
                list.add(i);
            }
        }
        log.info("误判的总数量：:{}",list.size());
    }
}
