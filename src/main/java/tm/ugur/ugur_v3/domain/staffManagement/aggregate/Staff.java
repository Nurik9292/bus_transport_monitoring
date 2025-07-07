package tm.ugur.ugur_v3.domain.staffManagement.aggregate;

import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.staffManagement.enums.StaffRole;
import tm.ugur.ugur_v3.domain.staffManagement.enums.StaffStatus;
import tm.ugur.ugur_v3.domain.staffManagement.events.*;
import tm.ugur.ugur_v3.domain.staffManagement.exceptions.*;
import tm.ugur.ugur_v3.domain.staffManagement.services.PasswordPolicy;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Staff {
    private StaffId id;
    private StaffName name;
    private Email email;
    private HashedPassword password;
    private StaffRole role;
    private StaffStatus status;
    private Avatar avatar;
    private SecuritySettings securitySettings;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Staff() {}


    public static Staff create(StaffName name, Email email, PlainPassword plainPassword,
                               StaffRole role, StaffId createdBy) {

        validateStaffCreationRules(name, email, role);

        Staff staff = new Staff();
        staff.id = StaffId.generate();
        staff.name = name;
        staff.email = email;
        staff.password = HashedPassword.fromPlain(plainPassword);
        staff.role = role;
        staff.status = StaffStatus.ACTIVE;
        staff.securitySettings = SecuritySettings.defaultSettings();
        staff.createdAt = LocalDateTime.now();


        staff.addDomainEvent(new StaffCreatedEvent(staff.id, name, email, role, createdBy));

        return staff;
    }


    public void updateProfile(StaffName newName, Email newEmail, StaffId updatedBy) {

        if (!this.role.canBeUpdatedBy(updatedBy)) {
            throw new InsufficientPermissionsException("Cannot update staff profile");
        }

        StaffName oldName = this.name;
        Email oldEmail = this.email;

        this.name = newName;
        this.email = newEmail;


        addDomainEvent(new StaffProfileUpdatedEvent(this.id, oldName, newName,
                oldEmail, newEmail, updatedBy));
    }


    public void changeRole(StaffRole newRole, StaffId changedBy) {

        if (!changedBy.isSuperAdmin()) {
            throw new InsufficientPermissionsException("Only SUPER_ADMIN can change roles");
        }

        StaffRole oldRole = this.role;
        this.role = newRole;

        addDomainEvent(new StaffRoleChangedEvent(this.id, oldRole, newRole, changedBy));
    }


    public void changePassword(PlainPassword currentPassword, PlainPassword newPassword,
                               StaffId changedBy) {

        if (!this.password.matches(currentPassword)) {
            throw new InvalidPasswordException("Current password is incorrect");
        }


        if (!PasswordPolicy.isValid(newPassword.value())) {
            throw new WeakPasswordException("Password does not meet security requirements");
        }

        this.password = HashedPassword.fromPlain(newPassword);
        this.securitySettings = this.securitySettings.passwordChanged();

        addDomainEvent(new StaffPasswordChangedEvent(this.id, changedBy));
    }


    public void deactivate(StaffId deactivatedBy, String reason) {
        if (this.status == StaffStatus.INACTIVE) {
            throw new StaffAlreadyInactiveException("Staff is already inactive");
        }

        this.status = StaffStatus.INACTIVE;

        addDomainEvent(new StaffDeactivatedEvent(this.id, deactivatedBy, reason));
    }


    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.securitySettings = this.securitySettings.loginRecorded();

        addDomainEvent(new StaffLoginRecordedEvent(this.id, this.lastLoginAt));
    }


    private static void validateStaffCreationRules(StaffName name, Email email, StaffRole role) {
        if (name.value().length() < 2) {
            throw new InvalidStaffNameException("Staff name must be at least 2 characters");
        }

        if (!email.isValid()) {
            throw new InvalidEmailException("Invalid email format");
        }

        if (role == StaffRole.SUPER_ADMIN) {
            throw new InvalidRoleAssignmentException("SUPER_ADMIN role cannot be assigned directly");
        }
    }

    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
