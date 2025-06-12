Medical Diagnosis Queue Management System

🚀 Mô tả dự án

Hệ thống quản lý khám bệnh đa tenant (đa cơ sở y tế) hỗ trợ đặt lịch khám, phân phòng tự động, và xử lý khám song song bằng đa luồng. Mỗi tenant có database schema riêng, sử dụng dynamic routing để đảm bảo dữ liệu cách ly.

🏗️ Kiến trúc tổng thể

Spring Boot (3.5+) + Hibernate

Multi-tenant theo schema

Database: MySQL

🩺 Chức năng chính

1. Quản lý Tenant

Tạo tenant mới với schema riêng

Tự động khởi tạo bảng và dữ liệu mẫu (admin, role, phòng khám,...)
