package no.fintlabs

import no.fintlabs.branding.BrandingController
import no.fintlabs.branding.FrontEndBrandingConfiguration
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

class FrontEndBrandingControllerSpec extends Specification{
    FrontEndBrandingConfiguration frontEndBrandingConfiguration
    def setup(){
        frontEndBrandingConfiguration = new FrontEndBrandingConfiguration()
    }

    def "returns primaryColor"(){
        given:
        frontEndBrandingConfiguration.setPrimaryColor("7a1668")

        when:
        var primaryColor = frontEndBrandingConfiguration.getPrimaryColor()

        then:
        primaryColor == "7a1668"
    }

}
