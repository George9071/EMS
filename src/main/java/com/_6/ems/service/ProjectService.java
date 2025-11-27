package com._6.ems.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.request.ProjectUpdateRequest;
import com._6.ems.dto.response.EmployeeSimpleResponse;
import com._6.ems.entity.*;
import com._6.ems.enums.ProjectStatus;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.EmployeeMapper;
import com._6.ems.mapper.ProjectMapper;
import com._6.ems.repository.*;
import com._6.ems.utils.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import com._6.ems.dto.response.ProjectResponse;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectService {

    ProjectRepository projectRepository;
    DepartmentRepository departmentRepository;
    EmployeeRepository employeeRepository;
    EmployeeProjectRepository employeeProjectRepository;
    ManagerRepository managerRepository;
    ProjectMapper projectMapper;
    EmployeeMapper employeeMapper;

    @Transactional
    public ProjectResponse createProject(ProjectCreationRequest request) {
        Department department = departmentRepository.findById(getDepartmentIdOfCurrentManager())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        Project project = projectMapper.toProject(request);
        project.setDepartment(department);
        project.setStatus(ProjectStatus.PLANNED);

        return projectMapper.toProjectResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(String id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new AppException(ErrorCode.INVALID_DATE_RANGE);
            }
        }

        projectMapper.updateProject(request, project);
        projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    public ProjectResponse getProjectById(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        return projectMapper.toProjectResponse(project);
    }

    public List<ProjectResponse> getProjectsByDepartment(int departmentID) {
        Department department = departmentRepository.findById(departmentID)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        List<Project> projects = projectRepository.findByDepartment(department);
        return projects.stream().
                map(projectMapper::toProjectResponse)
                .collect(Collectors.toList());
    }

    public List<ProjectResponse> searchByStatus(ProjectStatus status) {
        if (SecurityUtil.isCurrentUserAdmin()) {
            return projectRepository.findByStatus(status)
                    .stream()
                    .map(projectMapper::toProjectResponse)
                    .toList();
        }

        return projectRepository.findByStatusAndDepartment_Id(status, getDepartmentIdOfCurrentManager())
                .stream()
                .map(projectMapper::toProjectResponse)
                .toList();
    }

    public List<ProjectResponse> searchByStartDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (to.isBefore(from)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (SecurityUtil.isCurrentUserAdmin()) {
            return projectRepository.findByStartDateBetween(from, to)
                    .stream()
                    .map(projectMapper::toProjectResponse)
                    .toList();
        }

        return projectRepository.findByStartDateBetweenAndDepartment_Id(from, to, getDepartmentIdOfCurrentManager())
                .stream()
                .map(projectMapper::toProjectResponse)
                .toList();
    }

    @Transactional
    public void deleteProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        projectRepository.delete(project);
    }

    @Transactional
    public List<EmployeeSimpleResponse> assignEmployeeToProject(String projectId, String employeeCode) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        Employee employee = employeeRepository.findById(employeeCode)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Department projectDept = project.getDepartment();
        if (projectDept == null) throw new AppException(ErrorCode.PROJECT_NOT_ASSIGNED_TO_DEPARTMENT);

        Department employeeDept = employee.getDepartment();
        if (employeeDept == null) throw new AppException(ErrorCode.EMPLOYEE_NOT_ASSIGNED_TO_DEPARTMENT);

        if (!Objects.equals(projectDept.getId(), employeeDept.getId())) {
            throw new AppException(ErrorCode.EMPLOYEE_NOT_IN_DEPARTMENT);
        }

        boolean assigned = employeeProjectRepository
                .existsByEmployee_CodeAndProject_Id(employeeCode, projectId);

        if (!assigned) {
            int currentParticipants = project.getEmployees().size();
            if (currentParticipants >= project.getMaxParticipants()) {
                throw new AppException(ErrorCode.EXCEED_MAX_PARTICIPANTS);
            }

            EmployeeProject member = EmployeeProject.builder()
                    .employee(employee)
                    .project(project)
                    .joinedAt(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                    .build();

            project.getEmployees().add(member);
            project.setParticipants(project.getEmployees().size());
        }

        projectRepository.save(project);

        List<Employee> employees = project.getEmployees().stream()
                .map(EmployeeProject::getEmployee)
                .toList();

        return employeeMapper.toSimpleResponseList(employees);
    }

    @Transactional
    public List<EmployeeSimpleResponse> removeEmployeeFromProject(String projectId, String employeeCode) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        Employee employee = employeeRepository.findById(employeeCode)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        EmployeeProject member = project.getEmployees().stream()
                .filter(assignment -> assignment.getEmployee().getCode().equals(employeeCode))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_IN_PROJECT));


        project.getEmployees().remove(member);
        employee.getProjects().remove(member);

        project.setParticipants(project.getEmployees().size());

        projectRepository.save(project);

        List<Employee> employees = project.getEmployees().stream()
                .map(EmployeeProject::getEmployee)
                .distinct()
                .toList();

        return employeeMapper.toSimpleResponseList(employees);
    }

    public List<EmployeeSimpleResponse> getEmployeesInProject(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        List<Employee> employees = project.getEmployees().stream()
                .map(EmployeeProject::getEmployee)
                .toList();

        return employeeMapper.toSimpleResponseList(employees);
    }

    private Integer getDepartmentIdOfCurrentManager() {
        String code = SecurityUtil.getCurrentUserCode();

        Manager manager = managerRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.MANAGER_NOT_FOUND));

        if (manager.getDepartment() == null) throw new AppException(ErrorCode.MANAGER_NOT_MANAGE_ANY_DEPARTMENT);

        return manager.getDepartment().getId();
    }
}
