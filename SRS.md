# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)
## ĐẶC TẢ YÊU CẦU PHẦN MỀM - PHÂN HỆ TẤT TOÁN SỔ TIẾT KIỆM (TERM DEPOSIT SETTLEMENT)

**Tên dự án:** Hệ thống Core Banking Ngân hàng Thương mại  
**Mã bài thi / Repository:** `HCM-K24-CNTT1_PhamHuynhTienDat_008`  
**Sinh viên thực hiện:** Phạm Huỳnh Tiến Đạt  
**Phiên bản:** 1.0.0 (Chính thức)  

---

### 1. GIỚI THIỆU VÀ TỔNG QUAN (INTRODUCTION & SYSTEM OVERVIEW)

#### 1.1. Mục đích (Purpose)
Tài liệu SRS này quy định chi tiết các đặc tả yêu cầu chức năng, yêu cầu phi chức năng, kiến trúc mô hình dữ liệu và quy tắc xử lý ngoại lệ cho **Phân hệ Tất toán Sổ tiết kiệm (Term Deposit Settlement Module)** thuộc hệ thống Core Banking.

#### 1.2. Phạm vi hệ thống (Scope)
Hệ thống hỗ trợ quản lý các sổ tiết kiệm có kỳ hạn (1 tháng, 6 tháng, 12 tháng), cho phép khách hàng thực hiện mở sổ và tất toán sổ tiết kiệm tại bất kỳ thời điểm nào. Hệ thống tự động tính toán số ngày gửi thực tế, áp dụng logic rẽ nhánh lãi suất (Tất toán đúng hạn vs Tất toán trước hạn) để đảm bảo tính chính xác tuyệt đối, tránh thất thoát tài chính của ngân hàng.

---

### 2. YÊU CẦU NGHIỆP VỤ CỐT LÕI (CORE BUSINESS REQUIREMENTS)

#### 2.1. Quản lý Vòng đời Sổ tiết kiệm (Term Deposit Lifecycle)
1. Một sổ tiết kiệm (`TermDeposit`) được khởi tạo ở trạng thái **`ACTIVE`**, liên kết với một Khách hàng (`Customer`) hoặc Tài khoản thanh toán (`BankAccount`).
2. Ngày đáo hạn (`maturityDate`) được tự động gán bằng: `depositDate.plusMonths(termMonths)`.
3. Kỳ hạn gửi mặc định hỗ trợ: 1 tháng, 6 tháng, 12 tháng cùng mức lãi suất có kỳ hạn thỏa thuận ban đầu ($r_{termed}$).

#### 2.2. Thuật toán Rẽ nhánh Lãi suất Tất toán (Settlement Interest Branching Algorithm)
Khi API Tất toán được kích hoạt tại ngày tất toán thực tế (`settlementDate`):
* **Trường hợp A: Rút đúng hoặc sau ngày đáo hạn (`settlementDate >= maturityDate`)**
  * Khách hàng hưởng nguyên **Lãi suất có kỳ hạn ban đầu** ($r_{termed}$).
  * Số ngày gửi thực tế ($d$) được tính từ `depositDate` đến `settlementDate`.
  * Công thức tiền lãi:
    $$\text{InterestEarned} = \frac{\text{PrincipalAmount} \times r_{termed} \times d}{365}$$

* **Trường hợp B: Rút trước ngày đáo hạn (`settlementDate < maturityDate`)**
  * Dù chỉ rút trước 1 ngày, toàn bộ tiền lãi sẽ bị tính lại theo **Lãi suất không kỳ hạn** ($r_{demand} = 0.1\%/\text{năm} = 0.001$).
  * Số ngày gửi thực tế ($d$) được tính từ `depositDate` đến `settlementDate`.
  * Công thức tiền lãi:
    $$\text{InterestEarned} = \frac{\text{PrincipalAmount} \times 0.001 \times d}{365}$$

#### 2.3. Ràng buộc An toàn & Xử lý Ngoại lệ (Business Constraints & Exceptions)
* **Quy tắc 01 lần tất toán:** Một sổ tiết kiệm chỉ được phép tất toán **đúng 01 lần**.
* **Xử lý tất toán lại (Re-settlement Protection):** Nếu một sổ tiết kiệm đã ở trạng thái `SETTLED` mà tiếp tục nhận được yêu cầu tất toán:
  * Hệ thống chặn giao dịch và ném ngoại lệ `BusinessException`.
  * Mã lỗi HTTP Response: **`400 Bad Request`**.
  * Thông báo lỗi JSON chính xác: **`"Sổ tiết kiệm đã được tất toán trước đó"`**.

---

### 3. THIẾT KẾ MÔ HÌNH DỮ LIỆU (DATA MODEL & ENTITY SPECIFICATION)

#### 3.1. Entity `TermDeposit` (Bảng `term_deposits`)
| Thuộc tính (Attribute) | Kiểu dữ liệu Java | Tên cột DB | Ràng buộc DB | Diễn giải |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `Long` | `id` | `PRIMARY KEY, AUTO_INCREMENT` | Khóa chính sổ tiết kiệm |
| `customer` | `Customer` | `customer_id` | `FOREIGN KEY (customers)` | Khách hàng chủ sổ |
| `bankAccount` | `BankAccount` | `bank_account_id` | `FOREIGN KEY (bank_accounts)` | Tài khoản liên kết tất toán |
| `principalAmount` | `BigDecimal` | `principal_amount` | `NOT NULL, NUMERIC(19,4)` | Số tiền gửi gốc |
| `interestRate` | `BigDecimal` | `interest_rate` | `NOT NULL, NUMERIC(7,4)` | Lãi suất kỳ hạn (%/năm) |
| `termMonths` | `Integer` | `term_months` | `NOT NULL` | Kỳ hạn (1, 6, 12 tháng) |
| `depositDate` | `LocalDate` | `deposit_date` | `NOT NULL, DATE` | Ngày mở sổ tiết kiệm |
| `maturityDate` | `LocalDate` | `maturity_date` | `NOT NULL, DATE` | Ngày đáo hạn |
| `settlementDate` | `LocalDate` | `settlement_date` | `NULLABLE, DATE` | Ngày tất toán thực tế |
| `interestEarned` | `BigDecimal` | `interest_earned` | `NULLABLE, NUMERIC(19,4)` | Số tiền lãi tính được |
| `settlementAmount` | `BigDecimal` | `settlement_amount` | `NULLABLE, NUMERIC(19,4)` | Tổng tiền nhận (Gốc + Lãi) |
| `status` | `TermDepositStatus` | `status` | `NOT NULL, VARCHAR(20)` | Enum: `ACTIVE`, `SETTLED` |

---

### 4. ĐẶC TẢ CHI TIẾT RESTFUL API (API SPECIFICATION)

#### 4.1. API Mở sổ tiết kiệm
- **URL:** `/api/v1/term-deposits`
- **Method:** `POST`
- **Request Body JSON:**
  ```json
  {
    "bankAccountId": 1,
    "principalAmount": 100000000,
    "termMonths": 6,
    "interestRate": 0.065,
    "depositDate": "2026-01-01"
  }
  ```
- **Response HTTP 201 Created:**
  ```json
  {
    "code": 200,
    "message": "Mở sổ tiết kiệm thành công",
    "data": {
      "id": 1,
      "principalAmount": 100000000,
      "interestRate": 0.065,
      "termMonths": 6,
      "depositDate": "2026-01-01",
      "maturityDate": "2026-07-01",
      "status": "ACTIVE"
    }
  }
  ```

#### 4.2. API Tất toán sổ tiết kiệm (Term Deposit Settlement)
- **URL:** `/api/v1/term-deposits/{id}/settle`
- **Method:** `POST`
- **Request Body JSON (Optional):**
  ```json
  {
    "settlementDate": "2026-07-01"
  }
  ```

##### 🟢 Success Response (HTTP 200 OK - Tất toán thành công):
```json
{
  "code": 200,
  "message": "Tất toán sổ tiết kiệm thành công",
  "data": {
    "id": 1,
    "principalAmount": 100000000,
    "interestRate": 0.065,
    "depositDate": "2026-01-01",
    "maturityDate": "2026-07-01",
    "settlementDate": "2026-07-01",
    "interestEarned": 3223287.67,
    "settlementAmount": 103223287.67,
    "status": "SETTLED",
    "appliedInterestRate": 0.065,
    "isEarlySettlement": false
  }
}
```

##### 🔴 Error Response (HTTP 400 Bad Request - Tất toán lại):
```json
{
  "data": null,
  "message": "Sổ tiết kiệm đã được tất toán trước đó",
  "code": 400
}
```

---

### 5. YÊU CẦU KỸ THUẬT VÀ ĐỘ CHÍNH XÁC TÀI CHÍNH (TECHNICAL REQUIREMENTS)
1. **Kiểu dữ liệu tài chính:** Sử dụng `BigDecimal` với chế độ làm tròn `RoundingMode.HALF_UP` cho toàn bộ phép tính gốc/lãi để triệt tiêu lỗi làm tròn dấu phẩy động.
2. **Tính toán thời gian:** Dùng thư viện Java `java.time.LocalDate` và `java.time.temporal.ChronoUnit.DAYS` tính số ngày gửi thực tế chính xác theo lịch vạn niên.
3. **Môi trường:** Java 17+, Spring Boot 3.x, Spring Data JPA, Gradle 8.x.
