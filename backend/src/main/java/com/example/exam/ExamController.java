package com.example.exam;

import com.example.dto.request.ExamSubmitRequest;
import com.example.dto.response.ExamRecordResponse;
import com.example.exam.entity.ExamRecord;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.security.StudentAccessService;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private StudentAccessService studentAccessService;

    /**
     * 提交考试成绩
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseResult<ExamRecordResponse> submitExam(@Valid @RequestBody ExamSubmitRequest submitRequest) {
        Long studentId = studentAccessService.requireCurrentStudentId();
        ExamRecord record = examService.submitExam(studentId, submitRequest);
        return ResponseResult.success(ExamRecordResponse.fromEntity(record));
    }

    /**
     * 获取考试记录列表
     */
    @GetMapping("/records/{studentId}")
    public ResponseResult<List<ExamRecordResponse>> getExamRecords(@PathVariable Long studentId) {
        studentAccessService.assertReadable(studentId);
        List<ExamRecordResponse> records = examService.getExamRecords(studentId);
        return ResponseResult.success(records);
    }

    /**
     * 获取考试详情
     */
    @GetMapping("/detail/{examId}")
    public ResponseResult<ExamRecordResponse> getExamDetail(@PathVariable Long examId) {
        ExamRecordResponse detail = examService.getExamDetail(examId);
        return ResponseResult.success(detail);
    }
}
