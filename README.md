🏥 Medical Diagnosis Queue Management System

🚀 Giới thiệu dự án
Hệ thống quản lý khám bệnh đa tenant (nhiều cơ sở y tế), hỗ trợ:

Đặt lịch khám bệnh

Phân phòng tự động dựa trên số lượng phòng rảnh

Xử lý luồng khám bệnh song song bằng đa luồng

Mỗi tenant có schema cơ sở dữ liệu riêng biệt

Sử dụng Dynamic Routing DataSource để tách biệt dữ liệu giữa các tenant một cách an toàn

🏗️ Kiến trúc tổng thể
Backend: Spring Boot 3.5+ với Hibernate 6.6

Kiến trúc đa tenant: Mỗi tenant sử dụng một schema riêng biệt

Database: MySQL

Threading: Java multithreading cho phân phòng khám

Authentication: JWT + RBAC

