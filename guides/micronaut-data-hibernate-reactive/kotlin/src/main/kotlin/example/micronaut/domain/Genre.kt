package example.micronaut.domain

import io.micronaut.serde.annotation.Serdeable
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Serdeable
@Entity
class Genre(@Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            val id: Long?,

            @NotBlank
            var name: String
)