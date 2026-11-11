package uz.melarium.repository;

import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByPhone(String phone);
    boolean existsByEmailIgnoreCaseAndPhoneNot(String email, String phone);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    long countByAccountStatus(String accountStatus);

    @Query("""
            select user from UserAccount user
            where (:query = '' or lower(user.phone) like lower(concat('%', :query, '%'))
                   or lower(coalesce(user.fullName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(user.email, '')) like lower(concat('%', :query, '%')))
              and (:status is null or user.accountStatus = :status)
            order by user.createdAt desc
            """)
    Page<UserAccount> searchUsers(@Param("query") String query, @Param("status") String status, Pageable pageable);
}
