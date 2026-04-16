package com.kato.pro.langchain.chat.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电商助手工具集 - 展示复杂的 Function Tools 实现
 * 
 * 功能包括：
 * 1. 商品查询（支持多条件筛选）
 * 2. 订单创建（幂等性保证）
 * 3. 库存检查（模拟外部API调用）
 * 4. 用户积分查询（多租户隔离）
 */
@Slf4j
@Component
public class ECommerceTools {
    
    // 模拟商品数据库（实际应从数据库读取）
    private static final Map<String, Product> PRODUCT_DB = new ConcurrentHashMap<>();
    
    // 模拟订单数据库（实际应从数据库读取）
    private static final Map<String, List<Order>> ORDER_DB = new ConcurrentHashMap<>();
    
    // 模拟积分数据库（实际应从数据库读取）
    private static final Map<String, Integer> POINTS_DB = new ConcurrentHashMap<>();
    
    static {
        // 初始化模拟数据
        PRODUCT_DB.put("1", new Product("1", "iPhone 15 Pro", "智能手机", 7999.00, 50));
        PRODUCT_DB.put("2", new Product("2", "MacBook Pro 14", "笔记本电脑", 14999.00, 30));
        PRODUCT_DB.put("3", new Product("3", "AirPods Pro", "耳机", 1899.00, 100));
        PRODUCT_DB.put("4", new Product("4", "iPad Air", "平板电脑", 4799.00, 45));
        PRODUCT_DB.put("5", new Product("5", "Apple Watch", "智能手表", 2999.00, 80));
    }
    
    // ==================== 1. 商品查询工具（复杂参数） ====================
    
    /**
     * 查询商品 - 支持按名称、分类、价格区间筛选
     * 
     * @param memoryId 用户ID（自动透传，用于多租户隔离） 框架透传的，不会被劫持
     * @param keyword  搜索关键词
     * @param category 商品分类（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @return 符合条件的商品列表（JSON格式）
     */
    @Tool("根据关键词、分类、价格区间查询商品信息。返回商品ID、名称、价格、库存。")
    public String searchProducts(
            @ToolMemoryId String memoryId,
            @P("搜索关键词，如：手机、电脑、耳机") String keyword,
            @P(value = "商品分类，可选值：智能手机、笔记本电脑、耳机、平板电脑、智能手表", required = false) String category,
            @P(value = "最低价格（元）", required = false) Double minPrice,
            @P(value = "最高价格（元）", required = false) Double maxPrice
    ) {
        log.info("[User: {}] 查询商品 - keyword: {}, category: {}, price: {}-{}", 
                 memoryId, keyword, category, minPrice, maxPrice);
        
        try {
            List<Product> results = new ArrayList<>();
            
            for (Product product : PRODUCT_DB.values()) {
                // 关键词匹配（名称或分类包含关键词）
                boolean keywordMatch = keyword == null || 
                        product.getName().contains(keyword) || 
                        product.getCategory().contains(keyword);
                
                // 分类匹配
                boolean categoryMatch = category == null || 
                        product.getCategory().equals(category);
                
                // 价格区间匹配
                boolean priceMatch = true;
                if (minPrice != null && product.getPrice() < minPrice) {
                    priceMatch = false;
                }
                if (maxPrice != null && product.getPrice() > maxPrice) {
                    priceMatch = false;
                }
                
                if (keywordMatch && categoryMatch && priceMatch) {
                    results.add(product);
                }
            }
            
            if (results.isEmpty()) {
                return "未找到符合条件的商品。建议：放宽搜索条件或尝试其他关键词。";
            }
            
            // 转换为 JSON 返回（便于 LLM 理解）
            return convertToJson(results);
            
        } catch (Exception e) {
            log.error("商品查询失败", e);
            return "查询商品时发生错误：" + e.getMessage();
        }
    }
    
    
    // ==================== 2. 创建订单工具（幂等性保证） ====================
    
    /**
     * 创建订单 - 包含幂等性设计，防止重复下单
     * 
     * @param memoryId    用户ID（用于数据隔离）
     * @param productId   商品ID
     * @param quantity    购买数量
     * @param requestId   请求唯一标识（由调用方生成，用于幂等）
     * @param address     收货地址
     * @param phone       联系电话
     */
    @Tool("创建订单购买商品。需要提供商品ID、数量、收货地址和电话。订单创建成功后返回订单号。")
    public String createOrder(
            @ToolMemoryId String memoryId,
            @P("商品ID，可通过searchProducts查询获取") String productId,
            @P("购买数量，必须大于0") Integer quantity,
            @P(value = "请求唯一标识，用于防止重复下单（建议使用UUID）", required = false) String requestId,
            @P("收货地址") String address,
            @P("联系电话") String phone
    ) {
        log.info("[User: {}] 创建订单 - productId: {}, quantity: {}, requestId: {}", 
                 memoryId, productId, quantity, requestId);
        
        // 1. 参数校验
        if (quantity == null || quantity <= 0) {
            return "错误：购买数量必须大于0";
        }
        if (address == null || address.trim().isEmpty()) {
            return "错误：收货地址不能为空";
        }
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return "错误：联系电话格式不正确（应为11位手机号）";
        }
        
        // 2. 幂等性检查：如果提供了 requestId，检查是否已处理过
        String idempotentKey = memoryId + ":" + requestId;
        if (requestId != null && isRequestProcessed(idempotentKey)) {
            String existingOrderNo = getExistingOrderNo(idempotentKey);
            log.info("[User: {}] 重复请求拦截 - requestId: {}, 已有订单: {}", 
                     memoryId, requestId, existingOrderNo);
            return "该请求已处理，订单号为：" + existingOrderNo;
        }
        
        // 3. 检查商品是否存在
        Product product = PRODUCT_DB.get(productId);
        if (product == null) {
            return "错误：商品不存在，商品ID：" + productId;
        }
        
        // 4. 检查库存
        if (product.getStock() < quantity) {
            return String.format("错误：商品 [%s] 库存不足。当前库存：%d，需要：%d",
                    product.getName(), product.getStock(), quantity);
        }
        
        try {
            // 5. 扣减库存（实际场景中需要加锁或使用数据库乐观锁）
            product.setStock(product.getStock() - quantity);
            
            // 6. 创建订单
            String orderNo = generateOrderNo(memoryId);
            double totalAmount = product.getPrice() * quantity;
            
            Order order = Order.builder()
                    .orderNo(orderNo)
                    .userId(memoryId)
                    .productId(productId)
                    .productName(product.getName())
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .totalAmount(totalAmount)
                    .address(address)
                    .phone(phone)
                    .status("PENDING")
                    .createTime(LocalDateTime.now())
                    .build();
            
            // 存储订单
            ORDER_DB.computeIfAbsent(memoryId, k -> new ArrayList<>()).add(order);
            
            // 7. 记录幂等标识
            if (requestId != null) {
                markRequestProcessed(idempotentKey, orderNo);
            }
            
            log.info("[User: {}] 订单创建成功 - orderNo: {}, totalAmount: {}", 
                     memoryId, orderNo, totalAmount);
            
            // 8. 返回友好信息
            return String.format("订单创建成功！\n" +
                    "订单号：%s\n" +
                    "商品：%s\n" +
                    "数量：%d\n" +
                    "单价：¥%.2f\n" +
                    "总金额：¥%.2f\n" +
                    "收货地址：%s\n" +
                    "预计发货时间：24小时内",
                    orderNo, product.getName(), quantity, 
                    product.getPrice(), totalAmount, address);
                    
        } catch (Exception e) {
            log.error("[User: {}] 创建订单失败", memoryId, e);
            return "创建订单失败：" + e.getMessage();
        }
    }
    
    
    // ==================== 3. 库存检查工具（模拟外部API调用） ====================
    
    /**
     * 检查商品库存 - 模拟调用外部仓储系统API
     * 
     * @param memoryId  用户ID
     * @param productId 商品ID
     * @param quantity  需要检查的数量
     */
    @Tool("检查指定商品的库存是否充足。返回库存状态和当前库存数量。")
    public String checkStock(
            @ToolMemoryId String memoryId,
            @P("商品ID") String productId,
            @P(value = "需要检查的数量", required = false) Integer quantity
    ) {
        log.info("[User: {}] 检查库存 - productId: {}, quantity: {}", memoryId, productId, quantity);
        
        Product product = PRODUCT_DB.get(productId);
        if (product == null) {
            return "错误：商品不存在，商品ID：" + productId;
        }
        
        int currentStock = product.getStock();
        boolean sufficient = quantity == null || currentStock >= quantity;
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("商品：%s\n", product.getName()));
        result.append(String.format("当前库存：%d\n", currentStock));
        
        if (quantity != null) {
            if (sufficient) {
                result.append(String.format("库存充足，可以购买 %d 件。", quantity));
            } else {
                result.append(String.format("库存不足，当前仅剩 %d 件，无法购买 %d 件。", 
                        currentStock, quantity));
            }
        }
        
        return result.toString();
    }
    
    
    // ==================== 4. 用户积分查询（多租户隔离） ====================
    
    /**
     * 查询用户积分 - 演示多租户数据隔离
     * 
     * @param memoryId 用户ID（自动从 AiService 透传，确保数据隔离）
     */
    @Tool("查询当前用户的积分余额。积分可用于抵扣订单金额（100积分=1元）。")
    public String getUserPoints(
            @ToolMemoryId String memoryId
    ) {
        log.info("[User: {}] 查询积分", memoryId);
        
        // 从数据库获取积分（演示多租户隔离：不同 memoryId 返回不同数据）
        Integer points = POINTS_DB.computeIfAbsent(memoryId, k -> {
            // 新用户赠送100积分
            return 100;
        });
        
        return String.format("当前积分：%d 分（可抵扣 ¥%.2f）", 
                points, points / 100.0);
    }
    
    
    // ==================== 辅助方法 ====================
    
    /**
     * 生成订单号
     * 格式：ORD + 年月日时分秒毫秒 + 用户ID后4位
     */
    private String generateOrderNo(String userId) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String userSuffix = userId.length() > 4 ? 
                userId.substring(userId.length() - 4) : 
                String.format("%04d", Integer.parseInt(userId));
        return "ORD" + timestamp + userSuffix;
    }
    
    /**
     * 将商品列表转换为 JSON 字符串
     */
    private String convertToJson(List<Product> products) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append(String.format("  {\"id\":\"%s\", \"name\":\"%s\", \"category\":\"%s\", \"price\":%.2f, \"stock\":%d}",
                    p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock()));
            if (i < products.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
    
    // 幂等性存储（实际应使用 Redis）
    private static final Map<String, String> IDEMPOTENT_STORE = new ConcurrentHashMap<>();
    
    private boolean isRequestProcessed(String key) {
        return IDEMPOTENT_STORE.containsKey(key);
    }
    
    private void markRequestProcessed(String key, String orderNo) {
        IDEMPOTENT_STORE.put(key, orderNo);
        // 实际场景中设置过期时间，如30分钟
    }
    
    private String getExistingOrderNo(String key) {
        return IDEMPOTENT_STORE.get(key);
    }
    
    
    // ==================== 内部实体类 ====================
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Product {
        private String id;
        private String name;
        private String category;
        private Double price;
        private int stock;
    }
    
    @lombok.Data
    @lombok.Builder
    private static class Order {
        private String orderNo;
        private String userId;
        private String productId;
        private String productName;
        private Integer quantity;
        private Double unitPrice;
        private Double totalAmount;
        private String address;
        private String phone;
        private String status;
        private LocalDateTime createTime;
    }
}