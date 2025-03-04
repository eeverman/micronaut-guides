package example.micronaut

import grails.gorm.MultiTenant
import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity
import io.micronaut.core.annotation.Introspected

@Introspected
@Entity // <1>
class Book implements GormEntity<Book>, // <2>
                MultiTenant { // <3>
    String title

    static constraints = {
        title nullable: false, blank: false
    }
}
