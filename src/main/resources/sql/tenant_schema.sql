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
-- TABLE: specializations
CREATE TABLE IF NOT EXISTS specializations (
                                               id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
    );

-- TABLE: departments
CREATE TABLE IF NOT EXISTS departments (
                                           id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    room_number VARCHAR(255),
    type VARCHAR(255),
    specialization_id VARCHAR(36),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_specialization
    FOREIGN KEY (specialization_id)
    REFERENCES specializations(id)
    ON DELETE SET NULL
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
    staff_code VARCHAR(50) NOT NULL UNIQUE,
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    gender VARCHAR(50),
    dob DATE,
    account_id VARCHAR(36),
    department_id VARCHAR(36),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_staff_department
    FOREIGN KEY (department_id)
    REFERENCES departments(id)
    ON DELETE SET NULL
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
    specialization_id CHAR(36),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (specialization_id) REFERENCES specializations(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (queue_id) REFERENCES daily_queues(id)
    );

CREATE TABLE IF NOT EXISTS invoices (
                                        id CHAR(36) PRIMARY KEY,
    invoice_code VARCHAR(100) NOT NULL UNIQUE,
    patient_id CHAR(36) NOT NULL,
    total DECIMAL(15, 2),
    original_total DECIMAL(15, 2), -- Tổng tiền ban đầu trước giảm giá và thuế
    vat_total DECIMAL(15, 2), -- Tiền thuế
    discount_total DECIMAL(15, 2), -- Tiền giảm giá
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
    CONSTRAINT fk_medical_orders_creator FOREIGN KEY (created_by) REFERENCES staffs(id)
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

-- TABLE: medical_result_images
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

-- TABLE: template_files
CREATE TABLE IF NOT EXISTS template_files (
                                              id VARCHAR(36) PRIMARY KEY,
    type ENUM('MEDICAL_RECORD', 'INVOICE', 'EMAIL') NOT NULL,
    name VARCHAR(255),
    file_url TEXT NOT NULL,
    description TEXT,
    preview_url TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
    );

-- TABLE: email_tasks
CREATE TABLE IF NOT EXISTS email_tasks (
                                           id VARCHAR(255) PRIMARY KEY,
    email_to VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    retry_count INT DEFAULT 0,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
    );
-- table: shifts
CREATE TABLE IF NOT EXISTS shifts (
                                      id CHAR(36) PRIMARY KEY,
                                      name VARCHAR(255) NOT NULL UNIQUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
    );


-- TABLE: work_schedules
CREATE TABLE IF NOT EXISTS work_schedules (
                                              id CHAR(36) PRIMARY KEY,
    staff_id CHAR(36) NOT NULL,
    shift_date DATE,
    shift VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    note TEXT,
    check_in_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,

    CONSTRAINT fk_work_schedule_staff FOREIGN KEY (staff_id) REFERENCES staffs(id)
    );

CREATE TABLE IF NOT EXISTS leave_requests (
                                              id CHAR(36) PRIMARY KEY,
    staff_id CHAR(36) NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,

    CONSTRAINT fk_leave_requests_staff FOREIGN KEY (staff_id) REFERENCES staffs(id)
    );

CREATE TABLE IF NOT EXISTS leave_request_details (
                                                     id CHAR(36) PRIMARY KEY,
    leave_request_id CHAR(36) NOT NULL,
    date DATE NOT NULL,
    shift VARCHAR(20) NOT NULL,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,

    CONSTRAINT fk_leave_request_details_request FOREIGN KEY (leave_request_id) REFERENCES leave_requests(id)
    );

CREATE TABLE IF NOT EXISTS service_packages (
    id CHAR(36) PRIMARY KEY,
    package_name VARCHAR(255) NOT NULL,
    description TEXT,

    billing_type VARCHAR(50) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,

    status VARCHAR(50) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME
);

CREATE TABLE IF NOT EXISTS settings (
                                        id VARCHAR(36) PRIMARY KEY,
    hospital_name VARCHAR(255),
    hospital_phone VARCHAR(50),
    hospital_email VARCHAR(255),
    hospital_address VARCHAR(255),
    bank_account_number VARCHAR(100),
    bank_code VARCHAR(100),
    latest_check_in_minutes INT,
    pagination_size_list TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL
);

