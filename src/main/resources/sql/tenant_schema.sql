-- TABLE: accounts
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- TABLE: roles
CREATE TABLE IF NOT EXISTS roles (
    name VARCHAR(100) PRIMARY KEY,
    description VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- TABLE: permissions
CREATE TABLE IF NOT EXISTS permissions (
    name VARCHAR(100) PRIMARY KEY,
    description VARCHAR(255),
    group_name VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

INSERT IGNORE INTO permissions (name, description, group_name, created_at, updated_at)
VALUES
-- Biên lai thu tiền
('view:receipt', 'Xem biên lai thu tiền', 'Biên lai thu tiền', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('create:receipt', 'Thêm biên lai thu tiền', 'Biên lai thu tiền', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('update:receipt', 'Cập nhật biên lai thu tiền', 'Biên lai thu tiền', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('delete:receipt', 'Xóa biên lai thu tiền', 'Biên lai thu tiền', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tài khoản
('view:account', 'Xem tài khoản', 'Tài khoản', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('create:account', 'Thêm tài khoản', 'Tài khoản', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('update:account', 'Cập nhật tài khoản', 'Tài khoản', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('delete:account', 'Xóa tài khoản', 'Tài khoản', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Nhân viên
('view:staff', 'Xem thông tin nhân viên', 'Nhân viên', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('create:staff', 'Thêm nhân viên', 'Nhân viên', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('update:staff', 'Cập nhật thông tin nhân viên', 'Nhân viên', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('delete:staff', 'Xóa nhân viên', 'Nhân viên', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Bệnh nhân
('view:patient', 'Xem thông tin bệnh nhân', 'Bệnh nhân', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('create:patient', 'Thêm bệnh nhân', 'Bệnh nhân', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('update:patient', 'Cập nhật bệnh nhân', 'Bệnh nhân', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('delete:patient', 'Xóa bệnh nhân', 'Bệnh nhân', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- TABLE: account_roles
CREATE TABLE IF NOT EXISTS account_roles (
    account_id VARCHAR(36) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (account_id, role_name),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (role_name) REFERENCES roles(name) ON DELETE CASCADE
);

-- TABLE: role_permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    role_name VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_name, permission_name),
    FOREIGN KEY (role_name) REFERENCES roles(name) ON DELETE CASCADE,
    FOREIGN KEY (permission_name) REFERENCES permissions(name) ON DELETE CASCADE
);

-- TABLE: invalidated_tokens
CREATE TABLE IF NOT EXISTS invalidated_tokens (
    id VARCHAR(100) PRIMARY KEY,
    expire_time TIMESTAMP
);

-- INSERT default roles
INSERT INTO roles (name, description) VALUES ('USER', 'User role')
    ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'Admin role'),
                                          ('STAFF', 'Staff role'),
                                          ('PATIENT', 'Patient role')
    ON DUPLICATE KEY UPDATE description = VALUES(description);

-- INSERT admin account
INSERT INTO accounts (id, username, password)
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'admin', '$2a$10$jmjiQYFQ/.4rf6ruJNPnUOYPIoGBiurHq2Y3BRG1Zg0RiAsd/neqy')
    ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO account_roles (account_id, role_name)
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'ADMIN')
    ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- TABLE: departments
CREATE TABLE IF NOT EXISTS departments (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    room_number VARCHAR(255),
    type VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- TABLE: medical_service
CREATE TABLE IF NOT EXISTS medical_service (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    department_id VARCHAR(36) NOT NULL,
    price NUMERIC(15, 3) NOT NULL,
    discount NUMERIC(5, 2),
    vat NUMERIC(3, 1) NOT NULL CHECK (vat IN (0, 8, 10)),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY(department_id) REFERENCES departments(id)
);

-- TABLE: staffs
CREATE TABLE IF NOT EXISTS staffs (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    specialty VARCHAR(255),
    level VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    gender VARCHAR(50),
    dob DATE,
    account_id VARCHAR(36),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- TABLE: department_staffs
CREATE TABLE IF NOT EXISTS department_staffs (
    id VARCHAR(36) PRIMARY KEY,
    department_id VARCHAR(36) NOT NULL,
    staff_id VARCHAR(36) NOT NULL,
    position VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_department FOREIGN KEY(department_id) REFERENCES departments(id),
    CONSTRAINT fk_staff FOREIGN KEY(staff_id) REFERENCES staffs(id)
);

-- TABLE: patients
CREATE TABLE IF NOT EXISTS patients (
    id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(50) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(255) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

INSERT INTO accounts (id, username, password, created_at, updated_at)
VALUES
    ('acc-0001', 'an.nguyen', '$2a$10$xFNYSXylcV3KtUtdC.uvzuEeNMvi93WM7U0Z/jHMpSRLvftE1F0/W', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('acc-0002', 'binh.tran', '$2a$10$xFNYSXylcV3KtUtdC.uvzuEeNMvi93WM7U0Z/jHMpSRLvftE1F0/W', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('acc-0003', 'cuong.le', '$2a$10$xFNYSXylcV3KtUtdC.uvzuEeNMvi93WM7U0Z/jHMpSRLvftE1F0/W', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('acc-0004', 'dieu.pham', '$2a$10$xFNYSXylcV3KtUtdC.uvzuEeNMvi93WM7U0Z/jHMpSRLvftE1F0/W', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('acc-0005', 'dung.hoang', '$2a$10$xFNYSXylcV3KtUtdC.uvzuEeNMvi93WM7U0Z/jHMpSRLvftE1F0/W', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON DUPLICATE KEY UPDATE
                         password = VALUES(password),
                         updated_at = CURRENT_TIMESTAMP;

INSERT INTO patients (id, first_name, middle_name, last_name, dob, gender, phone, email, account_id, created_at, updated_at)
VALUES
    ('a1e1c1d1-b2f2-4e3f-a4f4-5a6a7b8c9d0e', 'Nguyen', 'Van', 'An', '1990-01-15', 'Male', '0912345678', 'an.nguyen@example.com', 'acc-0001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b2e2c2d2-c3f3-5e4f-b5f5-6b7b8c9d0e1f', 'Tran', 'Thi', 'Binh', '1985-03-22', 'Female', '0933456789', 'binh.tran@example.com', 'acc-0002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c3e3c3d3-d4f4-6e5f-c6f6-7c8c9d0e1f2g', 'Le', 'Minh', 'Cuong', '1992-07-09', 'Male', '0944567890', 'cuong.le@example.com', 'acc-0003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d4e4c4d4-e5f5-7e6f-d7f7-8d9d0e1f2g3h', 'Pham', 'Ngoc', 'Dieu', '1998-11-30', 'Female', '0955678901', 'dieu.pham@example.com', 'acc-0004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e5e5c5d5-f6f6-8e7f-e8f8-9e0e1f2g3h4i', 'Hoang', 'Anh', 'Dung', '2000-06-05', 'Male', '0966789012', 'dung.hoang@example.com', 'acc-0005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p001', 'Nguyen', 'Thanh', 'Tung', '1991-01-01', 'Male', '0901000001', 'tung.nguyen@example.com', 'acc-0001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p002', 'Le', 'Thi', 'Hoa', '1988-02-15', 'Female', '0901000002', 'hoa.le@example.com', 'acc-0002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p003', 'Tran', 'Minh', 'Khoa', '1995-04-20', 'Male', '0901000003', 'khoa.tran@example.com', 'acc-0003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p004', 'Pham', 'Ngoc', 'Mai', '1993-07-12', 'Female', '0901000004', 'mai.pham@example.com', 'acc-0004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p005', 'Hoang', 'Van', 'Linh', '1987-11-11', 'Male', '0901000005', 'linh.hoang@example.com', 'acc-0005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p006', 'Bui', 'Thanh', 'Tam', '1996-05-09', 'Female', '0901000006', 'tam.bui@example.com', 'acc-0001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p007', 'Do', 'Minh', 'Anh', '1992-09-19', 'Male', '0901000007', 'anh.do@example.com', 'acc-0002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p008', 'Nguyen', 'Thi', 'Dao', '1990-06-25', 'Female', '0901000008', 'dao.nguyen@example.com', 'acc-0003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p009', 'Tran', 'Quoc', 'Phong', '1989-08-14', 'Male', '0901000009', 'phong.tran@example.com', 'acc-0004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p010', 'Vo', 'Nhat', 'Ha', '1994-12-03', 'Female', '0901000010', 'ha.vo@example.com', 'acc-0005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p011', 'Le', 'Quang', 'Duc', '1997-03-01', 'Male', '0901000011', 'duc.le@example.com', 'acc-0001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p012', 'Ngo', 'Thi', 'Thu', '1998-10-10', 'Female', '0901000012', 'thu.ngo@example.com', 'acc-0002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p013', 'Dang', 'Van', 'Nam', '1986-04-04', 'Male', '0901000013', 'nam.dang@example.com', 'acc-0003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p014', 'Huynh', 'Thi', 'Yen', '1991-01-09', 'Female', '0901000014', 'yen.huynh@example.com', 'acc-0004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('p015', 'Trinh', 'Hoang', 'Long', '1993-06-06', 'Male', '0901000015', 'long.trinh@example.com', 'acc-0005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON DUPLICATE KEY UPDATE
                         first_name = VALUES(first_name),
                         middle_name = VALUES(middle_name),
                         last_name = VALUES(last_name),
                         dob = VALUES(dob),
                         gender = VALUES(gender),
                         phone = VALUES(phone),
                         email = VALUES(email),
                         updated_at = CURRENT_TIMESTAMP;

-- TABLE: daily_queues
CREATE TABLE IF NOT EXISTS daily_queues (
    id VARCHAR(36) PRIMARY KEY,
    queue_date DATE NOT NULL,
    status VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

INSERT INTO daily_queues (id, queue_date, status, created_at, updated_at)
VALUES
    ('q001', CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

-- TABLE: queue_patients (mapping bệnh nhân -> hàng đợi)
CREATE TABLE IF NOT EXISTS queue_patients (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    queue_id VARCHAR(36) NOT NULL,
    status VARCHAR(50),
    queue_order BIGINT,
    checkin_time TIMESTAMP,
    checkout_time TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (queue_id) REFERENCES daily_queues(id)
);

INSERT IGNORE INTO queue_patients (id, patient_id, queue_id, status, queue_order, checkin_time, checkout_time, created_at, updated_at)
VALUES
    ('qp001', 'p001', 'q001', 'WAITING', 1, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp002', 'p002', 'q001', 'WAITING', 2, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp003', 'p003', 'q001', 'WAITING', 3, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp004', 'p004', 'q001', 'WAITING', 4, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp005', 'p005', 'q001', 'WAITING', 5, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp006', 'p006', 'q001', 'WAITING', 6, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp007', 'p007', 'q001', 'WAITING', 7, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp008', 'p008', 'q001', 'WAITING', 8, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp009', 'p009', 'q001', 'WAITING', 9, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp010', 'p010', 'q001', 'WAITING', 10, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp011', 'p011', 'q001', 'WAITING', 11, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp012', 'p012', 'q001', 'WAITING', 12, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp013', 'p013', 'q001', 'WAITING', 13, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp014', 'p014', 'q001', 'WAITING', 14, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp015', 'p015', 'q001', 'WAITING', 15, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp016', 'a1e1c1d1-b2f2-4e3f-a4f4-5a6a7b8c9d0e', 'q001', 'WAITING', 16, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp017', 'b2e2c2d2-c3f3-5e4f-b5f5-6b7b8c9d0e1f', 'q001', 'WAITING', 17, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp018', 'c3e3c3d3-d4f4-6e5f-c6f6-7c8c9d0e1f2g', 'q001', 'WAITING', 18, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp019', 'd4e4c4d4-e5f5-7e6f-d7f7-8d9d0e1f2g3h', 'q001', 'WAITING', 19, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('qp020', 'e5e5c5d5-f6f6-8e7f-e8f8-9e0e1f2g3h4i', 'q001', 'WAITING', 20, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE queue_patients
SET status = 'WAITING',
    checkin_time = CURRENT_TIMESTAMP
WHERE queue_order <= 15;
