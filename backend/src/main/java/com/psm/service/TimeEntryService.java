package com.psm.service;

import com.psm.dto.TimeEntryRequest;
import com.psm.dto.TimeEntryResponse;
import com.psm.entity.Task;
import com.psm.entity.TimeEntry;
import com.psm.entity.User;
import com.psm.repository.TaskRepository;
import com.psm.repository.TimeEntryRepository;
import com.psm.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TaskRepository taskRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> findAll(Long taskId) {
        requireTask(taskId);
        return timeEntryRepository.findByTaskIdOrderByEntryDateDescIdDesc(taskId).stream()
            .map(TimeEntryResponse::from)
            .toList();
    }

    @Transactional
    public TimeEntryResponse create(Long taskId, TimeEntryRequest request, String userEmail) {
        Task task = requireTask(taskId);
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        TimeEntry entry = new TimeEntry();
        entry.setTask(task);
        entry.setUser(user);
        entry.setHours(request.hours());
        entry.setDescription(blankToNull(request.description()));
        entry.setEntryDate(request.entryDate());
        return TimeEntryResponse.from(timeEntryRepository.save(entry));
    }

    @Transactional
    public void delete(Long taskId, Long entryId, String userEmail, boolean admin) {
        TimeEntry entry = timeEntryRepository.findByIdAndTaskId(entryId, taskId)
            .orElseThrow(() -> new RuntimeException("Time entry not found"));

        if (!admin && !entry.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("You cannot delete this time entry");
        }
        timeEntryRepository.delete(entry);
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
