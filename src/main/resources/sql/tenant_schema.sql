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
INSERT IGNORE INTO permissions (
  name, description, group_name, created_at,
  updated_at
)
VALUES
  -- Biên lai thu tiền
  (
    'view:receipt', 'Xem biên lai thu tiền',
    'Biên lai thu tiền', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'create:receipt', 'Thêm biên lai thu tiền',
    'Biên lai thu tiền', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'update:receipt', 'Cập nhật biên lai thu tiền',
    'Biên lai thu tiền', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'delete:receipt', 'Xóa biên lai thu tiền',
    'Biên lai thu tiền', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  -- Tài khoản
  (
    'view:account', 'Xem tài khoản',
    'Tài khoản', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'create:account', 'Thêm tài khoản',
    'Tài khoản', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'update:account', 'Cập nhật tài khoản',
    'Tài khoản', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'delete:account', 'Xóa tài khoản',
    'Tài khoản', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  -- Nhân viên
  (
    'view:staff', 'Xem thông tin nhân viên',
    'Nhân viên', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'create:staff', 'Thêm nhân viên',
    'Nhân viên', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'update:staff', 'Cập nhật thông tin nhân viên',
    'Nhân viên', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'delete:staff', 'Xóa nhân viên',
    'Nhân viên', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  -- Bệnh nhân
  (
    'view:patient', 'Xem thông tin bệnh nhân',
    'Bệnh nhân', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'create:patient', 'Thêm bệnh nhân',
    'Bệnh nhân', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'update:patient', 'Cập nhật bệnh nhân',
    'Bệnh nhân', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'delete:patient', 'Xóa bệnh nhân',
    'Bệnh nhân', CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  );
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
INSERT INTO roles (name, description)
VALUES
    ('USER', 'User role') ON DUPLICATE KEY
UPDATE
    description =
VALUES
    (description);
INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'Admin role'),
    ('STAFF', 'Staff role'),
    ('PATIENT', 'Patient role') ON DUPLICATE KEY
UPDATE
    description =
VALUES
    (description);
-- INSERT admin account
INSERT INTO accounts (id, username, password)
VALUES
    (
        '123e4567-e89b-12d3-a456-426614174000',
        'admin', '$2a$10$jmjiQYFQ/.4rf6ruJNPnUOYPIoGBiurHq2Y3BRG1Zg0RiAsd/neqy'
    ) ON DUPLICATE KEY
UPDATE
    username =
VALUES
    (username);
INSERT INTO account_roles (account_id, role_name)
VALUES
    (
        '123e4567-e89b-12d3-a456-426614174000',
        'ADMIN'
    ) ON DUPLICATE KEY
UPDATE
    role_name =
VALUES
    (role_name);
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
CREATE TABLE IF NOT EXISTS medical_services (
                                                id VARCHAR(36) PRIMARY KEY,
    service_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    department_id VARCHAR(36) NOT NULL,
    price NUMERIC(15, 3) NOT NULL,
    discount NUMERIC(5, 2),
    vat NUMERIC(3, 1) NOT NULL CHECK (
                                         vat IN (0, 8, 10)
    ),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY(department_id) REFERENCES departments(id)
    );
-- TABLE: staffs
CREATE TABLE IF NOT EXISTS staffs (
                                      id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
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
    patient_code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
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
-- TABLE: daily_queues
CREATE TABLE IF NOT EXISTS daily_queues (
    id VARCHAR(36) PRIMARY KEY,
    queue_date TIMESTAMP NOT NULL,
    status VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);
INSERT INTO daily_queues (
    id, queue_date, status, created_at,
    updated_at
)
VALUES
    (
        'q001', CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ) ON DUPLICATE KEY
UPDATE
    status = 'ACTIVE';
-- TABLE: queue_patients (mapping bệnh nhân -> hàng đợi)
CREATE TABLE IF NOT EXISTS queue_patients (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    queue_id VARCHAR(36) NOT NULL,
    room_number VARCHAR(36),
    type varchar(50),
    status VARCHAR(50),
    queue_order BIGINT,
    checkin_time TIMESTAMP,
    checkout_time TIMESTAMP,
    called_time TIMESTAMP,
    is_priority BOOLEAN,
    registered_time TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (queue_id) REFERENCES daily_queues(id)
);

CREATE TABLE IF NOT EXISTS invoices (
                                        id CHAR(36) PRIMARY KEY,
    invoice_code VARCHAR(100) NOT NULL UNIQUE,
    patient_id CHAR(36) NOT NULL,
    amount DECIMAL(15, 2),
    payment_type VARCHAR(50),
    description TEXT,
    status VARCHAR(20) NOT NULL,
    confirmed_by CHAR(36),
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_invoices_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_invoices_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES staffs(id)
    );
CREATE TABLE IF NOT EXISTS invoice_items (
                                             id CHAR(36) PRIMARY KEY,
    invoice_id CHAR(36) NOT NULL,
    service_type_id CHAR(36) NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    name VARCHAR(255),
    quantity INT NOT NULL,
    price DECIMAL(15, 2),
    discount DECIMAL(15, 2),
    vat DECIMAL(15, 2),
    total DECIMAL(15, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_invoice_items_service FOREIGN KEY (service_type_id) REFERENCES medical_services(id)
    );
CREATE TABLE IF NOT EXISTS medical_records (
                                               id CHAR(36) PRIMARY KEY,
    medical_record_code VARCHAR(100) NOT NULL UNIQUE,
    patient_id CHAR(36) NOT NULL,
    created_by CHAR(36) NOT NULL,
    diagnosis_text TEXT,
    summary TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_medical_records_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_medical_records_creator FOREIGN KEY (created_by) REFERENCES staffs(id)
    );
CREATE TABLE IF NOT EXISTS medical_orders (
                                              id CHAR(36) PRIMARY KEY,
    medical_record_id CHAR(36) NOT NULL,
    service_id CHAR(36) NOT NULL,
    invoice_item_id CHAR(36),
    created_by CHAR(36) NOT NULL,

    note TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_medical_orders_record FOREIGN KEY (medical_record_id) REFERENCES medical_records(id),
    CONSTRAINT fk_medical_orders_service FOREIGN KEY (service_id) REFERENCES medical_services(id),
    CONSTRAINT fk_medical_orders_invoice_item FOREIGN KEY (invoice_item_id) REFERENCES invoice_items(id),
    CONSTRAINT fk_medical_orders_creator FOREIGN KEY (created_by) REFERENCES staffs(id),
    );
-- TABLE: code_sequences
CREATE TABLE IF NOT EXISTS code_sequences (
                                              code_type VARCHAR(50) PRIMARY KEY,
    -- Loại mã (vd: MEDICAL_RECORD, INVOICE)
    current_value BIGINT NOT NULL -- Giá trị hiện tại (sẽ tăng dần mỗi lần sinh mã)
    );
-- TABLE: medical_result_images
CREATE TABLE IF NOT EXISTS medical_results (
                                               id CHAR(36) PRIMARY KEY,
    medical_order_id CHAR(36) NOT NULL,
    completed_by CHAR(36) NOT NULL,
    result_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_medical_results_medical_order
    FOREIGN KEY (medical_order_id) REFERENCES medical_orders(id),

    CONSTRAINT fk_medical_results_completed_by
    FOREIGN KEY (completed_by) REFERENCES staffs(id)
    );

-- TABLE: medical_result_i
CREATE TABLE IF NOT EXISTS medical_result_images (
                                                     id CHAR(36) PRIMARY KEY,
    medical_result_id CHAR(36) NOT NULL,
    image_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_result_images_result
    FOREIGN KEY (medical_result_id) REFERENCES medical_results(id)
    );
