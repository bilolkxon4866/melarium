package uz.melarium.service;

import java.util.UUID;

import uz.melarium.domain.Apiary;
import uz.melarium.repository.ApiaryRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiaryAccessService {
    private final ApiaryRepository apiaryRepository;

    public ApiaryAccessService(ApiaryRepository apiaryRepository) { this.apiaryRepository = apiaryRepository; }

    public Apiary requireAccess(UUID apiaryId, AuthenticatedUser actor) {
        Apiary apiary = apiaryRepository.findById(apiaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asalarixona topilmadi"));
        boolean administrator = actor.roles().contains("ADMIN") || actor.roles().contains("SUPER_ADMIN");
        if (!administrator && !apiary.getOwnerId().equals(actor.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asalarixona topilmadi");
        }
        return apiary;
    }
}
