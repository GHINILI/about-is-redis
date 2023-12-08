  @Test
    public void testGuavaWithBloomFilter()
    {
// 创建布隆过滤器对象
        BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), 100);
// 判断指定元素是否存在
        System.out.println(filter.mightContain(1));
        System.out.println(filter.mightContain(2));
// 将元素添加进布隆过滤器
        filter.put(1);
        filter.put(2);
//打印结果
        System.out.println(filter.mightContain(1));
        System.out.println(filter.mightContain(2));
    }
