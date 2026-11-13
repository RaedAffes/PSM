package com.psm.service;

import com.psm.dto.TaskCommentRequest;
import com.psm.dto.TaskCommentResponse;
import com.psm.entity.Task;
import com.psm.entity.TaskComment;
import com.psm.entity.User;
import com.psm.repository.TaskCommentRepository;
import com.psm.repository.TaskRepository;
import com.psm.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> findAll(Long taskId) {
        requireTask(taskId);
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId).stream()
            .map(TaskCommentResponse::from)
            .toList();
    }

    @Transactional
    public TaskCommentResponse create(Long taskId, TaskCommentRequest request, String authorEmail) {
        Task task = requireTask(taskId);
        User author = userRepository.findByEmail(authorEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        return TaskCommentResponse.from(taskCommentRepository.save(comment));
    }

    @Transactional
    public void delete(Long taskId, Long commentId, String authorEmail, boolean admin) {
        TaskComment comment = taskCommentRepository.findByIdAndTaskId(commentId, taskId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!admin && !comment.getAuthor().getEmail().equalsIgnoreCase(authorEmail)) {
            throw new RuntimeException("You cannot delete this comment");
        }
        taskCommentRepository.delete(comment);
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
    }
}
