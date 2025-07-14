package tm.ugur.ugur_v3.domain.shared.services;

public interface DomainService {

    default String getServiceDescription() {
        return getClass().getSimpleName();
    }
}