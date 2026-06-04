package com.edutask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootApplication
public class EdutaskApplication {

	public static void main(String[] args) {
		fixSqlServerIdentity();
		SpringApplication.run(EdutaskApplication.class, args);
	}

	private static void fixSqlServerIdentity() {
		String url = "jdbc:sqlserver://localhost:1433;databaseName=edutask_db;encrypt=true;trustServerCertificate=true;";
		String user = "sa";
		String password = "sa";

		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			try (Connection conn = DriverManager.getConnection(url, user, password);
				 Statement stmt = conn.createStatement()) {
				
				boolean checkNeeded = false;
				try (ResultSet rs = stmt.executeQuery("SELECT COLUMNPROPERTY(OBJECT_ID('transactions'), 'transaction_id', 'IsIdentity')")) {
					if (rs.next()) {
						Object val = rs.getObject(1);
						// Nếu bảng transactions tồn tại nhưng cột transaction_id không phải là cột IDENTITY (giá trị 0)
						if (val != null && (int) val == 0) {
							checkNeeded = true;
						}
					}
				}

				if (checkNeeded) {
					System.out.println(">>> [WARNING] PHAT HIEN BANG TRANSACTIONS THIEU THUOC TINH IDENTITY (LOI SCHEMA CUA SQL SERVER).");
					System.out.println(">>> DANG TIEN HANH RESET LAI TOAN BO CAU TRUC BANG DE HIBERNATE TAO LAI DUNG CHUAN IDENTITY...");
					
					// Xóa tất cả các khóa ngoại trước
					String dropFksSql = 
							"DECLARE @sql NVARCHAR(MAX) = N''; " +
							"SELECT @sql += N'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) " +
							"    + '.' + QUOTENAME(OBJECT_NAME(parent_object_id))  " +
							"    + ' DROP CONSTRAINT ' + QUOTENAME(name) + ';' " +
							"FROM sys.foreign_keys; " +
							"IF @sql <> N'' EXEC sp_executesql @sql;";
					stmt.execute(dropFksSql);

					// Xóa tất cả các bảng để Hibernate tạo lại chính xác với IDENTITY
					stmt.execute("DROP TABLE IF EXISTS activity_logs");
					stmt.execute("DROP TABLE IF EXISTS tokens");
					stmt.execute("DROP TABLE IF EXISTS transactions");
					stmt.execute("DROP TABLE IF EXISTS user_subscriptions");
					stmt.execute("DROP TABLE IF EXISTS tasks");
					stmt.execute("DROP TABLE IF EXISTS group_members");
					stmt.execute("DROP TABLE IF EXISTS groups");
					stmt.execute("DROP TABLE IF EXISTS users");
					stmt.execute("DROP TABLE IF EXISTS plans");
					
					System.out.println(">>> DA RESET DATABASE THANH CONG. UNG DUNG SE KHOI TAO LAI TOAN BO SCHEMA VA DU LIEU MAU MOI.");
				}
			}
		} catch (Exception e) {
			// Bỏ qua lỗi kết nối tại đây để Spring Boot báo cáo chi tiết hơn trong quá trình khởi tạo sau đó
			System.out.println(">>> Pre-startup database identity check completed: " + e.getMessage());
		}
	}
}
