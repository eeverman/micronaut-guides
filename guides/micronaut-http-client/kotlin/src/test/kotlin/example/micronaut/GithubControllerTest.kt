package example.micronaut

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxStreamingHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.stream.StreamSupport
import javax.inject.Inject

@MicronautTest // <1>
class GithubControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: RxStreamingHttpClient // <2>

    @Test
    fun verifyGithubReleasesCanBeFetchedWithLowLevelHttpClient() {
        //when:
        val request: HttpRequest<Any> = HttpRequest.GET("/github/releases-lowlevel")
        val rsp = client.toBlocking().exchange(request, // <3>
            Argument.listOf(GithubRelease::class.java)) // <4>

        //then: 'the endpoint can be accessed'
        assertEquals(HttpStatus.OK, rsp.status) // <5>
        assertNotNull(rsp.body()) // <6>

        //when:
        val releases = rsp.body()

        //then:
        for (name in expectedReleases) {
            assertTrue(releases.stream().map(GithubRelease::name).anyMatch { anObject: String? -> name.equals(anObject) })
        }
    }

    @Test
    fun verifyGithubReleasesCanBeFetchedWithCompileTimeAutoGeneratedAtClient() {
        //when:
        val request: HttpRequest<Any> = HttpRequest.GET("/github/releases-lowlevel")
        val githubReleaseStream = client.jsonStream(request, GithubRelease::class.java) // <7>
        val githubReleases = githubReleaseStream.blockingIterable()

        //then:
        for (name in expectedReleases) {
            assertTrue(StreamSupport.stream(githubReleases.spliterator(), false)
                .map(GithubRelease::name)
                .anyMatch { anObject: String? -> name.equals(anObject) })
        }
    }

    companion object {
        private val expectedReleases = listOf("Micronaut 2.5.0", "Micronaut 2.4.4", "Micronaut 2.4.3")
    }
}
