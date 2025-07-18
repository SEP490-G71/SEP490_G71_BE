package vn.edu.fpt.medicaldiagnosis.common;

import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.Role;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.RoleRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleMapperHelper {

    private final RoleRepository roleRepository;

    public RoleMapperHelper(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Set<Role> map(List<String> roleNames) {
        return roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND)))
                .collect(Collectors.toSet());
    }
}

