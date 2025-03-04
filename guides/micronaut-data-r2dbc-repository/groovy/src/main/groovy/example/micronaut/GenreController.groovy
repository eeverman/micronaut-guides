package example.micronaut

import io.micronaut.core.annotation.NonNull
import example.micronaut.domain.Genre
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import reactor.core.publisher.Mono

import javax.validation.Valid
import javax.validation.constraints.NotBlank

@Controller("/genres")  // <1>
class GenreController {

    protected final GenreRepository genreRepository

    GenreController(GenreRepository genreRepository) { // <2>
        this.genreRepository = genreRepository
    }

    @Get("/{id}") // <3>
    Mono<Genre> show(long id) {
        return genreRepository
                .findById(id) // <4>
    }

    @Put // <5>
    Mono<HttpResponse<?>> update(@Body @Valid GenreUpdateCommand command) { // <6>
        return genreRepository.update(command.id, command.name)
                .thenReturn(HttpResponse
                        .noContent()
                        .header(HttpHeaders.LOCATION, location(command.id).path))  // <7>
    }

    @Get("/list") // <8>
    Mono<List<Genre>> list(@Valid Pageable pageable) { // <9>
        return genreRepository.findAll(pageable)
                .map(Page::getContent)
    }

    @Post // <10>
    Mono<HttpResponse<Genre>> save(@Body("name") @NotBlank String name) {
        return genreRepository.save(name)
                .map(GenreController::createdGenre)
    }

    @Post("/ex") // <11>
    Mono<MutableHttpResponse<Genre>> saveExceptions(@Body @NotBlank String name) {
        return genreRepository.saveWithException(name)
                .map(GenreController::createdGenre)
                .onErrorReturn(HttpResponse.noContent())
    }

    @Delete("/{id}") // <12>
    @Status(HttpStatus.NO_CONTENT)
    Mono<Void> delete(long id) {
        return genreRepository.deleteById(id)
                .then()
    }

    @NonNull
    private static MutableHttpResponse<Genre> createdGenre(@NonNull Genre genre) {
        return HttpResponse
                .created(genre)
                .headers(headers -> headers.location(location(genre.getId())));
    }

    protected static URI location(Long id) {
        return URI.create("/genres/$id")
    }

    protected static URI location(Genre genre) {
        return location(genre.id)
    }
}
