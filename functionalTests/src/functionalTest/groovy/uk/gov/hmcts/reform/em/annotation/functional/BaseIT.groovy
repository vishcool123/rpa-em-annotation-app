package uk.gov.hmcts.reform.em.annotation.functional

import io.restassured.RestAssured
import net.jcip.annotations.NotThreadSafe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ContextConfiguration
import uk.gov.hmcts.reform.em.annotation.functional.config.FunctionalTestContextConfiguration

import javax.annotation.PostConstruct

import static io.restassured.RestAssured.given

@ContextConfiguration(classes = FunctionalTestContextConfiguration)
@NotThreadSafe
class BaseIT {

  @Autowired
  AuthTokenProvider authTokenProvider


  @Value('${base-urls.em-anno-app}')
  String emAnnoAppBaseUri

  @PostConstruct
  void init() {
    RestAssured.baseURI = emAnnoAppBaseUri
    RestAssured.useRelaxedHTTPSValidation()
  }

  def givenUnauthenticatedRequest() {
    def request = given().log().all()
    request
  }

  def givenRequest() {
    def request = given().log().all()
    request = request.header("ServiceAuthorization", serviceToken())
    request
  }

  def serviceToken() {
    authTokenProvider.findServiceToken()
  }

}
