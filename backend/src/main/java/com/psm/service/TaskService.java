package com.psm.service;

import com.psm.dto.TaskRequest;
import com.psm.dto.TaskResponse;
import com.psm.dto.TaskStats;
import com.psm.entity.Project;
import com.psm.entity.Task;
import com.psm.entity.TaskPriority;
import com.psm.entity.TaskStatus;
import com.psm.entity.User;
import com.psm.repository.ProjectRepository;
import com.psm.repository.TaskRepository;
import com.psm.repository.TimeEntryRepository;
import com.psm.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(Long projectId, TaskStatus status, Long assignedUserId) {
        return taskRepository.findAll().stream()
            .filter(task -> projectId == null
                || (task.getProject() != null && projectId.equals(task.getProject().getId())))
            .filter(task -> status == null || task.getStatus() == status)
            .filter(task -> assignedUserId == null
                || (task.getAssignee() != null && assignedUserId.equals(task.getAssignee().getId())))
            .map(TaskResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return TaskResponse.from(findTask(id));
    }

    @Transactional(readOnly = true)
    public TaskStats stats() {
        List<Task> tasks = taskRepository.findAll();
        long todo = tasks.stream().filter(task -> task.getStatus() == TaskStatus.TODO).count();
        long inProgress = tasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
        long done = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        long overdue = tasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.DONE)
            .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now()))
            .count();
        long urgent = tasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.DONE)
            .filter(task -> task.getPriority() == TaskPriority.URGENT)
            .count();
        BigDecimal totalHours = timeEntryRepository.findAll().stream()
            .map(entry -> entry.getHours() == null ? BigDecimal.ZERO : entry.getHours())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TaskStats(tasks.size(), todo, inProgress, done, overdue, urgent, totalHours);
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        apply(task, request, true);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findTask(id);
        apply(task, request, false);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = findTask(id);
        if (status == null) {
            throw new RuntimeException("Task status is required");
        }
        task.setStatus(status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(findTask(id));
    }

    private void apply(Task task, TaskRequest request, boolean creating) {
        Project project = projectRepository.findById(request.projectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        User assignee = null;
        if (request.assignedUserId() != null) {
            assignee = userRepository.findById(request.assignedUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        }

        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setProject(project);
        task.setAssignee(assignee);
        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());
        task.setStatus(request.status() == null
            ? (creating ? TaskStatus.TODO : task.getStatus())
            : request.status());
        task.setPriority(request.priority() == null
            ? (creating ? TaskPriority.MEDIUM : task.getPriority())
            : request.priority());
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
