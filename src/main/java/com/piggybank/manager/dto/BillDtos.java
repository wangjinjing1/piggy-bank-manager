package com.piggybank.manager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BillDtos {
    public static class BorrowRequest {
        @NotBlank
        private String borrowerName;
        private String status = "NORMAL";
        @NotBlank
        private String phone;
        @Email
        @NotBlank
        private String email;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;
        private LocalDate borrowDate;
        private LocalDate dueDate;
        @Size(max = 100)
        private String remark;

        public String getBorrowerName() { return borrowerName; }
        public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class DepositRequest {
        @NotBlank
        private String depositorName;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;
        @NotBlank
        private String bank;
        private LocalDate depositDate;
        @NotNull
        private LocalDate dueDate;
        private String status = "NORMAL";
        @Size(max = 100)
        private String remark;

        public String getDepositorName() { return depositorName; }
        public void setDepositorName(String depositorName) { this.depositorName = depositorName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getBank() { return bank; }
        public void setBank(String bank) { this.bank = bank; }
        public LocalDate getDepositDate() { return depositDate; }
        public void setDepositDate(LocalDate depositDate) { this.depositDate = depositDate; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class ReportQuery {
        private String type = "BORROW";
        private LocalDate startDate;
        private LocalDate endDate;
        private String name;
        private int page = 1;
        private int size = 10;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }

    public static class BorrowListQuery {
        private String borrowerName;
        private String phone;
        private String email;
        private LocalDate borrowStartDate;
        private LocalDate borrowEndDate;
        private LocalDate dueStartDate;
        private LocalDate dueEndDate;
        private int page = 1;
        private int size = 5;

        public String getBorrowerName() { return borrowerName; }
        public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public LocalDate getBorrowStartDate() { return borrowStartDate; }
        public void setBorrowStartDate(LocalDate borrowStartDate) { this.borrowStartDate = borrowStartDate; }
        public LocalDate getBorrowEndDate() { return borrowEndDate; }
        public void setBorrowEndDate(LocalDate borrowEndDate) { this.borrowEndDate = borrowEndDate; }
        public LocalDate getDueStartDate() { return dueStartDate; }
        public void setDueStartDate(LocalDate dueStartDate) { this.dueStartDate = dueStartDate; }
        public LocalDate getDueEndDate() { return dueEndDate; }
        public void setDueEndDate(LocalDate dueEndDate) { this.dueEndDate = dueEndDate; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }

    public static class DepositListQuery {
        private String depositorName;
        private String bank;
        private LocalDate depositStartDate;
        private LocalDate depositEndDate;
        private LocalDate dueStartDate;
        private LocalDate dueEndDate;
        private int page = 1;
        private int size = 5;

        public String getDepositorName() { return depositorName; }
        public void setDepositorName(String depositorName) { this.depositorName = depositorName; }
        public String getBank() { return bank; }
        public void setBank(String bank) { this.bank = bank; }
        public LocalDate getDepositStartDate() { return depositStartDate; }
        public void setDepositStartDate(LocalDate depositStartDate) { this.depositStartDate = depositStartDate; }
        public LocalDate getDepositEndDate() { return depositEndDate; }
        public void setDepositEndDate(LocalDate depositEndDate) { this.depositEndDate = depositEndDate; }
        public LocalDate getDueStartDate() { return dueStartDate; }
        public void setDueStartDate(LocalDate dueStartDate) { this.dueStartDate = dueStartDate; }
        public LocalDate getDueEndDate() { return dueEndDate; }
        public void setDueEndDate(LocalDate dueEndDate) { this.dueEndDate = dueEndDate; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }

    public static class RepaymentRequest {
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;
        private LocalDate repaymentDate;
        @Size(max = 100)
        private String remark;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getRepaymentDate() { return repaymentDate; }
        public void setRepaymentDate(LocalDate repaymentDate) { this.repaymentDate = repaymentDate; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class WithdrawalRequest {
        @NotBlank
        private String depositorName;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;
        private LocalDate withdrawalDate;
        @Size(max = 100)
        private String remark;

        public String getDepositorName() { return depositorName; }
        public void setDepositorName(String depositorName) { this.depositorName = depositorName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getWithdrawalDate() { return withdrawalDate; }
        public void setWithdrawalDate(LocalDate withdrawalDate) { this.withdrawalDate = withdrawalDate; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
