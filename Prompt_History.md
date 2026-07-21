# PROMPT HISTORY - NHẬT KÝ PROMPT ENGINEERING

**Mã bài thi / Repository:** `HCM-K24-CNTT1_PhamHuynhTienDat_008`  
**Dự án:** Core Banking - Phân hệ Tiết kiệm & Tất toán Sổ tiết kiệm  

---

### 📌 Prompt 1: Phân tích Đề thi & Khởi tạo Tài liệu SRS, Task Checklist

> **Role & Context:** Bạn là một Senior Core Banking Architect và AI Prompt Engineer trong lĩnh vực Tài chính - Ngân hàng.  
> **Task:** Hãy đọc đề thi của bài tập mã đề `HCM-K24-CNTT1_PhamHuynhTienDat_008` và phân tích toàn bộ yêu cầu nghiệp vụ.  
> **Requirements:**  
> • Phác thảo file `SRS.md` chi tiết theo chuẩn Banking bao gồm: Tổng quan, Đặc tả nghiệp vụ rẽ nhánh lãi suất tất toán, Entity `TermDeposit`, Đặc tả RESTful API, và Xử lý Ngoại lệ HTTP `400 Bad Request`.  
> • Phác thảo file `task.md` dưới dạng checklist các giai đoạn để quản lý tiến độ.  
> • Đảm bảo tính toán chính xác tiền lãi theo ngày gửi thực tế với `LocalDate` và `ChronoUnit.DAYS`.

- **Kỹ thuật Prompt:** Role-Playing + Task Decomposition  
- **Kết quả:** Tạo thành công 2 file `SRS.md` và `task.md` chuẩn chỉnh.

---

### 📌 Prompt 2: Khởi tạo TermDeposit Entity và JpaRepository

> **Role & Context:** Senior Java Developer.  
> **Task:** Tiến hành tạo mới Entity `TermDeposit` và `TermDepositRepository` trong package `com.banking.models`.  
> **Detailed Constraints:**  
> • Tạo Enum `TermDepositStatus` với 2 trạng thái: `ACTIVE`, `SETTLED`.  
> • Entity `TermDeposit` nằm ở package `com.banking.models.entities` với các thuộc tính: `id`, `customer`, `bankAccount`, `principalAmount`, `interestRate`, `termMonths`, `depositDate`, `maturityDate`, `settlementDate`, `interestEarned`, `settlementAmount`, `status`.  
> • Tạo `TermDepositRepository` kế thừa `JpaRepository<TermDeposit, Long>`.

- **Kỹ thuật Prompt:** Domain Model Enforcement + Data Type Mapping  
- **Kết quả:** Sinh mã nguồn `TermDepositStatus.java`, `TermDeposit.java`, `TermDepositRepository.java` chuẩn JPA.

---

### 📌 Prompt 3: Lập trình Business Logic Tất toán Sổ tiết kiệm (Service Layer)

> **Role & Context:** Core Banking Logic Developer.  
> **Task:** Hoàn thiện `TermDepositService` và `TermDepositServiceImpl` trong package `com.banking.models.services`.  
> **Requirements:**  
> • Hàm `openTermDeposit`: Tự động tính `maturityDate = depositDate.plusMonths(termMonths)` và lưu trạng thái `ACTIVE`.  
> • Hàm `settleTermDeposit`:  
>   - Nếu sổ có trạng thái `SETTLED` -> Ném `BusinessException(400, "Sổ tiết kiệm đã được tất toán trước đó")`.  
>   - Tính số ngày gửi thực tế bằng `ChronoUnit.DAYS.between(depositDate, settlementDate)`.  
>   - Logic rẽ nhánh lãi suất: Rút trước hạn (`settlementDate < maturityDate`) -> Áp dụng Lãi suất không kỳ hạn `0.1%/năm`. Rút đúng/sau hạn -> Áp dụng Lãi suất có kỳ hạn ban đầu.  
>   - Công thức tiền lãi: `(gốc * lãi_suất * số_ngày) / 365` tính bằng `BigDecimal` với `RoundingMode.HALF_UP`.  
>   - Cập nhật số tiền tất toán vào tài khoản liên kết nếu có.

- **Kỹ thuật Prompt:** Chain-of-Thought (CoT) + Exception Handling Guidance  
- **Kết quả:** Hoàn thành `TermDepositService.java` và `TermDepositServiceImpl.java` đạt chuẩn nghiệp vụ.

---

### 📌 Prompt 4: Tạo REST Controller và Cấu hình Security Permitted Endpoints

> **Role & Context:** API & Security Developer.  
> **Task:** Tạo `TermDepositController` và cập nhật `SecurityConfig`.  
> **Requirements:**  
> • Viết Controller `TermDepositController` tại `/api/v1/term-deposits`: `POST` mở sổ mới (`201 Created`), `POST /{id}/settle` tất toán sổ (`200 OK`), `GET /{id}` xem chi tiết, `GET` xem danh sách.  
> • Cập nhật `SecurityConfig.java`: Cho phép `permitAll()` với `/api/v1/term-deposits/**` để dễ dàng kiểm thử API.

- **Kỹ thuật Prompt:** API Design Best Practices + Security Policy Update  
- **Kết quả:** Tạo `TermDepositController.java` và cập nhật `SecurityConfig.java`.

---

### 📌 Prompt 5: Viết Unit Test Coverage cho các Kịch bản Nghiệp vụ

> **Role & Context:** QA / Test Engineer.  
> **Task:** Viết bộ kiểm thử `TermDepositServiceTest` bằng JUnit 5 và Mockito.  
> **Test Cases:**  
> • Tất toán đúng/sau ngày đáo hạn -> Khẳng định áp dụng lãi suất ban đầu, `isEarlySettlement = false`.  
> • Tất toán trước ngày đáo hạn -> Khẳng định áp dụng lãi suất `0.1%/năm`, `isEarlySettlement = true`.  
> • Tất toán lại sổ đã `SETTLED` -> Khẳng định ném `BusinessException` code `400` với thông báo `"Sổ tiết kiệm đã được tất toán trước đó"`.

- **Kỹ thuật Prompt:** Test-Driven Validation  
- **Kết quả:** Tạo file `TermDepositServiceTest.java` kiểm thử đầy đủ các trường hợp.
