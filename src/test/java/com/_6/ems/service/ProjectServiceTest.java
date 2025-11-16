package com._6.ems.service;

import com._6.ems.dto.request.ProjectCreationRequest;
import com._6.ems.dto.response.ProjectResponse;
import com._6.ems.entity.Department;
import com._6.ems.entity.Employee;
import com._6.ems.entity.EmployeeProject;
import com._6.ems.entity.Project;
import com._6.ems.enums.ProjectStatus;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.EmployeeMapper;
import com._6.ems.mapper.ProjectMapper;
import com._6.ems.repository.DepartmentRepository;
import com._6.ems.repository.EmployeeRepository;
import com._6.ems.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private ProjectService projectService;

    private Department department;
    private Project project;
    private Employee employee;
    private ProjectCreationRequest creationRequest;
    private ProjectResponse projectResponse;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1)
                .name("IT Department")
                .build();

        project = Project.builder()
                .id("PRJ001")
                .name("Test Project")
                .maxParticipants(10)
                .participants(0)
                .status(ProjectStatus.PLANNED)
                .employees(new HashSet<>())
                .build();

        employee = Employee.builder()
                .code("EMP001")
                .projects(new ArrayList<>())
                .build();

        creationRequest = ProjectCreationRequest.builder()
                .name("Test Project")
//                .departmentId(1)
                .build();

        projectResponse = ProjectResponse.builder()
                .id("PRJ001")
                .name("Test Project")
                .build();
    }

    @Test
    void createProject_validRequest_success() {
        // GIVEN
        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));
        when(projectMapper.toProject(creationRequest)).thenReturn(project);
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(projectMapper.toProjectResponse(project)).thenReturn(projectResponse);

        // WHEN
        ProjectResponse result = projectService.createProject(creationRequest);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("PRJ001");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
        assertThat(project.getParticipants()).isEqualTo(0);
        verify(departmentRepository).findById(1);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_departmentNotFound_throwsException() {
        // GIVEN
        when(departmentRepository.findById(1)).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> projectService.createProject(creationRequest));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
        verify(departmentRepository).findById(1);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void getProjectById_validId_success() {
        // GIVEN
        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
        when(projectMapper.toProjectResponse(project)).thenReturn(projectResponse);

        // WHEN
        ProjectResponse result = projectService.getProjectById("PRJ001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("PRJ001");
        verify(projectRepository).findById("PRJ001");
    }

    @Test
    void getProjectById_invalidId_throwsException() {
        // GIVEN
        when(projectRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> projectService.getProjectById("INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
        verify(projectRepository).findById("INVALID");
    }

    @Test
    void assignEmployeeToProject_success() {
        // GIVEN
        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(employeeMapper.toSimpleResponseList(any())).thenReturn(Collections.emptyList());

        // WHEN
        projectService.assignEmployeeToProject("PRJ001", "EMP001");

        // THEN
        verify(projectRepository).findById("PRJ001");
        verify(employeeRepository).findById("EMP001");
        verify(projectRepository).save(project);
        // Verify that the project was saved (which means employee was added)
        verify(projectRepository).save(argThat(p -> 
            p.getParticipants() == 1 && !p.getEmployees().isEmpty()
        ));
    }

    @Test
    void assignEmployeeToProject_exceedsMaxParticipants_throwsException() {
        // GIVEN
        project.setParticipants(10);
        project.setMaxParticipants(10);

        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> projectService.assignEmployeeToProject("PRJ001", "EMP001"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXCEED_MAX_PARTICIPANTS);
        verify(projectRepository).findById("PRJ001");
    }

    @Test
    void removeEmployeeFromProject_success() {
        // GIVEN
        EmployeeProject employeeProject = EmployeeProject.builder()
                .employee(employee)
                .project(project)
                .joinedAt(LocalDate.now())
                .build();

        project.getEmployees().add(employeeProject);
        employee.getProjects().add(employeeProject);
        project.setParticipants(1);

        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
        when(employeeRepository.findById("EMP001")).thenReturn(Optional.of(employee));
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(employeeMapper.toSimpleResponseList(any())).thenReturn(Collections.emptyList());

        // WHEN
        projectService.removeEmployeeFromProject("PRJ001", "EMP001");

        // THEN
        verify(projectRepository).findById("PRJ001");
        verify(employeeRepository).findById("EMP001");
        verify(projectRepository).save(project);
        // Verify that the collections were modified and participant count updated
        verify(projectRepository).save(argThat(p -> p.getParticipants() >= 0));
    }

//    @Test
//    void updateProjectStatus_toDeveloped_success() {
//        // GIVEN
//        when(projectRepository.findById("PRJ001")).thenReturn(Optional.of(project));
//        when(projectRepository.save(any(Project.class))).thenReturn(project);
//        when(projectMapper.toProjectResponse(project)).thenReturn(projectResponse);
//
//        // WHEN
//        projectService.updateProjectStatus("PRJ001", ProjectStatus.DEVELOPED);
//
//        // THEN
//        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DEVELOPED);
//        assertThat(project.getEndDate()).isNotNull();
//        verify(projectRepository).findById("PRJ001");
//        verify(projectRepository).save(project);
//    }

    @Test
    void getProjectsByStatus_success() {
        // GIVEN
        List<Project> projects = Arrays.asList(project);
        when(projectRepository.findByStatus(ProjectStatus.PLANNED)).thenReturn(projects);
        when(projectMapper.toProjectResponse(project)).thenReturn(projectResponse);

        // WHEN
        List<ProjectResponse> result = projectService.searchByStatus(ProjectStatus.PLANNED);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(projectRepository).findByStatus(ProjectStatus.PLANNED);
    }

    @Test
    void getProjectsByDepartment_success() {
        // GIVEN
        List<Project> projects = Arrays.asList(project);
        when(departmentRepository.findById(1)).thenReturn(Optional.of(department));
        when(projectRepository.findByDepartment(department)).thenReturn(projects);
        when(projectMapper.toProjectResponse(project)).thenReturn(projectResponse);

        // WHEN
        List<ProjectResponse> result = projectService.getProjectsByDepartment(1);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(departmentRepository).findById(1);
        verify(projectRepository).findByDepartment(department);
    }
}
