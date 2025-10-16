package com._6.ems.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.response.EmployeeResponse;
import com._6.ems.dto.response.EmployeeSimpleResponse;
import com._6.ems.enums.ProjectStatus;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.EmployeeMapper;
import com._6.ems.mapper.ProjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import com._6.ems.dto.response.ProjectResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.entity.Project;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.ProjectRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectService {

    ProjectRepository projectRepository;
    DepartmentRepository departmentRepository;
    EmployeeRepository employeeRepository;
    ProjectMapper projectMapper;
    EmployeeMapper employeeMapper;

    @Transactional
    public ProjectResponse createProject(ProjectCreationRequest request) {
        Department department = departmentRepository.findById(request.getDepartment_id())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Project project = projectMapper.toProject(request);
        project.setDepartment(department);
        project.setParticipants(0);
        project.setStatus(ProjectStatus.PLANNED);

        return projectMapper.toProjectResponse(projectRepository.save(project));
    }

    public ProjectResponse getProjectById(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        return projectMapper.toProjectResponse(project);
    }

    public List<ProjectResponse> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                .map(projectMapper::toProjectResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeSimpleResponse> getEmployeesInProject(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        return employeeMapper.toSimpleResponseList(project.getEmployees());
    }

    public List<EmployeeSimpleResponse> assignEmployeeToProject(String projectId, String employeeCode) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        Employee employee = employeeRepository.findById(employeeCode)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        if (project.getParticipants() >= project.getMaxParticipants()) {
            throw new AppException(ErrorCode.EXCEED_MAX_PARTICIPANTS);
        }

        if (!project.getEmployees().contains(employee)) {
            project.getEmployees().add(employee);
            employee.getProjects().add(project);
            project.setParticipants(project.getParticipants() + 1);
        }

        projectRepository.save(project);

        return employeeMapper.toSimpleResponseList(project.getEmployees());
    }

    public List<EmployeeSimpleResponse> removeEmployeeFromProject(String projectId, String employeeCode) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        Employee employee = employeeRepository.findById(employeeCode)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        if (project.getEmployees().remove(employee)) {
            employee.getProjects().remove(project);
            project.getEmployees().remove(employee);
            project.setParticipants(project.getEmployees().size());
        }

        projectRepository.save(project);

        return employeeMapper.toSimpleResponseList(project.getEmployees());
    }

    public ProjectResponse updateProjectStatus(String projectId, ProjectStatus newStatus) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        project.setStatus(newStatus);

        if (newStatus == ProjectStatus.DEVELOPED) {
            project.setEndDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        }

        return projectMapper.toProjectResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> getProjectsByDepartment(int departmentID) {
        Department department = departmentRepository.findById(departmentID)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        List<Project> projects = projectRepository.findByDepartment(department);
        return projects.stream().
                map(projectMapper::toProjectResponse)
                .collect(Collectors.toList());
    }
}
