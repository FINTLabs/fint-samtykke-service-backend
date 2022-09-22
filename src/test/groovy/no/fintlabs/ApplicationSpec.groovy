package no.fintlabs
import spock.lang.Specification

class ApplicationSpec extends Specification {

    def setupSpec() {}    // runs once -  before the first feature method
    def setup() {}        // runs before every feature method
    def cleanup() {}      // runs after every feature method
    def cleanupSpec() {}  // runs once -  after the last feature method

    def "Application is created"() {
        when:
        def application = new Application()

        then:
        application
    }
}