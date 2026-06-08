package com.edutask.config;

import com.edutask.entity.Plan;
import com.edutask.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        fixSqlServerDDLConstraints();
        fixIdentityColumns();

        if (planRepository.count() == 0) {
            Plan basic = Plan.builder()
                    .planName("Basic Student")
                    .price(new BigDecimal("19000"))
                    .currency("VND")
                    .durationDays(30)
                    .features("Quản lý 3 nhóm, Hạn chế quảng cáo, Gợi ý AI cơ bản, Theo dõi deadline")
                    .build();

            Plan standard = Plan.builder()
                    .planName("Standard Team")
                    .price(new BigDecimal("49000"))
                    .currency("VND")
                    .durationDays(90)
                    .features("Quản lý 10 nhóm, Gợi ý AI nâng cao, Báo cáo đóng góp chi tiết, Hỗ trợ 24/7")
                    .build();

            Plan pro = Plan.builder()
                    .planName("Pro Master")
                    .price(new BigDecimal("99000"))
                    .currency("VND")
                    .durationDays(180)
                    .features("Không giới hạn nhóm, AI phân công tối ưu, Ưu tiên hỗ trợ VIP, Tải báo cáo PDF")
                    .build();

            Plan elite = Plan.builder()
                    .planName("Elite Scholar")
                    .price(new BigDecimal("199000"))
                    .currency("VND")
                    .durationDays(360)
                    .features("Tất cả tính năng Pro, Thời gian sử dụng 1 năm, Giảm giá 50% gia hạn, Quản lý tài nguyên nhóm")
                    .build();

            planRepository.saveAll(List.of(basic, standard, pro, elite));
            System.out.println(">>> Đã khởi tạo các gói dịch vụ mặc định vào Database thành công!");
        }
    }

    private void fixSqlServerDDLConstraints() {
        try {
            // Fix activity_logs.ip_address default constraint & column type
            try {
                jdbcTemplate.execute(
                    "DECLARE @ConstraintName nvarchar(200); " +
                    "SELECT @ConstraintName = Name FROM sys.default_constraints " +
                    "WHERE parent_object_id = object_id('activity_logs') " +
                    "AND parent_column_id = Column_property(object_id('activity_logs'), 'ip_address', 'ColumnId'); " +
                    "IF @ConstraintName IS NOT NULL " +
                    "BEGIN " +
                    "   EXEC('ALTER TABLE activity_logs DROP CONSTRAINT ' + @ConstraintName); " +
                    "END"
                );
                
                jdbcTemplate.execute(
                    "ALTER TABLE activity_logs ALTER COLUMN ip_address VARCHAR(255)"
                );
                System.out.println(">>> Đã xử lý cột ip_address trong activity_logs thành công!");
            } catch (Exception e) {
                // Ignore if already fixed or not exists
            }

            // Fix group_members.contribution_score default constraint & column type
            try {
                jdbcTemplate.execute(
                    "DECLARE @ConstraintName2 nvarchar(200); " +
                    "SELECT @ConstraintName2 = Name FROM sys.default_constraints " +
                    "WHERE parent_object_id = object_id('group_members') " +
                    "AND parent_column_id = Column_property(object_id('group_members'), 'contribution_score', 'ColumnId'); " +
                    "IF @ConstraintName2 IS NOT NULL " +
                    "BEGIN " +
                    "   EXEC('ALTER TABLE group_members DROP CONSTRAINT ' + @ConstraintName2); " +
                    "END"
                );

                jdbcTemplate.execute(
                    "ALTER TABLE group_members ALTER COLUMN contribution_score INT"
                );
                System.out.println(">>> Đã xử lý cột contribution_score trong group_members thành công!");
            } catch (Exception e) {
                // Ignore if already fixed or not exists
            }
        } catch (Exception e) {
            System.err.println(">>> Lỗi khi dọn dẹp SQL Server constraints: " + e.getMessage());
        }
    }

    private void fixIdentityColumns() {
        try {
            // Check transactions table
            Integer txIdentityVal = null;
            try {
                txIdentityVal = jdbcTemplate.queryForObject(
                        "SELECT COLUMNPROPERTY(OBJECT_ID('transactions'), 'transaction_id', 'IsIdentity')",
                        Integer.class);
            } catch (Exception e) {
                // Table might not exist yet or return null
            }

            if (txIdentityVal != null && txIdentityVal == 0) {
                System.out.println(">>> Column 'transaction_id' is NOT identity. Recreating 'transactions' table...");
                jdbcTemplate.execute("DROP TABLE transactions");
                jdbcTemplate.execute(
                    "CREATE TABLE transactions (" +
                    "    transaction_id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "    amount DECIMAL(38,2) NOT NULL," +
                    "    created_at DATETIME2," +
                    "    payment_method VARCHAR(255)," +
                    "    status VARCHAR(255)," +
                    "    plan_id BIGINT NOT NULL," +
                    "    user_id BIGINT NOT NULL," +
                    "    CONSTRAINT FK_transactions_plans FOREIGN KEY (plan_id) REFERENCES plans(plan_id)," +
                    "    CONSTRAINT FK_transactions_users FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                    ")"
                );
                System.out.println(">>> Recreated 'transactions' table with IDENTITY column successfully!");
            }

            // Check user_subscriptions table
            Integer subIdentityVal = null;
            try {
                subIdentityVal = jdbcTemplate.queryForObject(
                        "SELECT COLUMNPROPERTY(OBJECT_ID('user_subscriptions'), 'subscription_id', 'IsIdentity')",
                        Integer.class);
            } catch (Exception e) {
                // Table might not exist yet or return null
            }

            if (subIdentityVal != null && subIdentityVal == 0) {
                System.out.println(">>> Column 'subscription_id' is NOT identity. Recreating 'user_subscriptions' table...");
                jdbcTemplate.execute("DROP TABLE user_subscriptions");
                jdbcTemplate.execute(
                    "CREATE TABLE user_subscriptions (" +
                    "    subscription_id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "    start_date DATETIME2," +
                    "    end_date DATETIME2," +
                    "    status VARCHAR(255)," +
                    "    updated_at DATETIME2," +
                    "    plan_id BIGINT NOT NULL," +
                    "    user_id BIGINT NOT NULL," +
                    "    CONSTRAINT FK_user_subscriptions_plans FOREIGN KEY (plan_id) REFERENCES plans(plan_id)," +
                    "    CONSTRAINT FK_user_subscriptions_users FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                    ")"
                );
                System.out.println(">>> Recreated 'user_subscriptions' table with IDENTITY column successfully!");
            }
        } catch (Exception e) {
            System.err.println(">>> Lỗi khi kiểm tra/sửa đổi cột IDENTITY: " + e.getMessage());
        }
    }
}
