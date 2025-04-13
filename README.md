# spring-boot-passkey-auth

Integrating Passkeys into Spring Security

1. Introduction
   Login forms have long been, and still are, a common feature of any web service that requires authentication to
   provide its services. However, as security concerns started to become mainstream, it became clear that simple text
   passwords are a weak spot: they can be guessed, intercepted, or leaked, leading to security incidents that can result
   in financial and/or reputation damage.

   Previous attempts to replace passwords with alternative solutions (mTLS, security cards, etc) tried to address this
   issue but led to poor user experience and additional costs.

   In this tutorial, we’ll explore Passkeys, also known as WebAuthn, a standard that provides a secure alternative to
   passwords. In particular, we’ll demonstrate how to quickly add support for this authentication mechanism to a Spring
   Boot application with Spring Security.

2. What Is a Passkey?
   Passkeys or WebAuthn is a standard API defined by the W3C Consortium that allows applications running on a Web
   Browser to manage public keys and register them for use with a given service provider.

   The typical registration scenario goes like this:

   The user creates a new account on the service. The initial credentials are usually the familiar username/password
   Once registered, the user goes to his profile page and selects “create passkey”
   The system displays a passkey registration form
   The user fills the form with the required information – e.g. the key label that will help the user select the right
   key later – and submits it
   The system saves the passkey in its database and associates it with the user account. At the same time, a private
   part of this key will be saved on the user’s device
   The passkey registration is complete
   Once key registration is completed, the user can use the stored passkey to access the service. Depending on the
   security configuration of the browser and the user’s device, the login will require a fingerprint scan, unlocking a
   smartphone, or similar action.

   A passkey consists of two parts: the public key that the browser sends to the service provider and a private part
   that remains on the local device.

   Moreover, the client-side API’s implementation ensures that a given passkey is usable only with the same site that
   registered it.

3. Adding Passkeys to Spring Boot Applications
   Let’s create a simple Spring Boot application to test passkeys. Our application will have just a welcome page that
   displays the name of the current user and a link to the passkey registration page.

The first step is to add the required dependencies to the project:

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.4.3</version>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-security</artifactId>
<version>3.4.3</version>
</dependency>
<dependency>
<groupId>com.webauthn4j</groupId>
<artifactId>webauthn4j-core</artifactId>
<version>0.28.5.RELEASE</version>
</dependency>
```

The latest versions of these dependencies are available on Maven Central:

* _**spring-boot-starter-web_**
* _**spring-boot-starter-security_**
* _**webauthn4j-core_**
  IMPORTANT: WebAuthn support requires Spring Boot version 3.4.0 or higher

4. Spring Security Configuration
   Starting with Spring Security 6.4, which is the default version included through the spring-boot-starter-security
   dependency, the configuration DSL comes with native support for passkeys through the webautn() method.
    ```java
       @Bean
    SecurityFilterChain webauthnFilterChain(HttpSecurity http, WebAuthNProperties webAuthNProperties) {
        return http.authorizeHttpRequests( ht -> ht.anyRequest().authenticated())
                .formLogin(withDefaults())
                .webAuthn(webauth ->
                        webauth.allowedOrigins(webAuthNProperties.getAllowedOrigins())
                                .rpId(webAuthNProperties.getRpId())
                                .rpName(webAuthNProperties.getRpName())
                )
                .build();
    }
    
    ```
   This is what we get with this configuration:

   A “login with passkey” button will be present on the login page
   A registration page available at /webauthn/register
   For proper operation, we must provide at least the following configuration attributes to the webauthn configurer:

   allowedOrigins: external URL of the site, which MUST use HTTPS, unless it uses localhost
   rpId: Application identifier, which MUST be a valid domain name that matches the hostname part of the allowedOrigin
   attribute
   rpName: A user-friendly name that the browser may use during the registration and/or login process
   This configuration, however, misses a critical aspect of passkey support: registered keys are lost upon application
   restart. This is because, by default, Spring Security uses a memory-based implementation credential store, which is
   not meant for production use.

   We’ll see how to fix this later on.

5. Passkey Walk-Around
   With the passkey configuration in place, it’s time for a quick walk-around through our application. Once we start it
   using mvn spring-boot:run or the IDE, we can open our browser and navigate to http://localhost:8080:

   Login form with passkey
   The standard login page for Spring applications will now include the “Sign in with a passkey” button. Since we
   haven’t registered any key yet, we must log in using username/password credentials, which we’ve configured in our
   application.yaml file: alice/changeit

   Welcome page
   As expected, we’re now logged in as Alice. We can now continue to the registration page by clicking on the Register
   PassKey link:

   Passkey registration page
   Here, we’ll just provide a label – baeldung-demo – and click on Register. What happens next depends on the device
   type (desktop, mobile, tablet) and OS (Windows, Linux, Mac, Android), but in the end, it will result in a new key
   being added to the list:

   Passkey registration success
   For instance, in Chrome on Windows, the dialog will give a choice to create a new key and store it with the browser’s
   native password manager or use the Windows Hello functionality available on the OS.

   Next, let’s log out of the application and try our new key. First, we navigate to http://localhost:8080/logout and
   confirm that want to exit. Next, on the login form, we click on “Sign in with a passkey”. The browser will show a
   dialog which allows you to select a passkey:

   Passkey Selector
   Once we select one of the available keys, the device will perform an additional authentication challenge. For the
   “Windows Hello” authentication, this can be a fingerprint scan, face recognition, etc.

   If the authentication is successful, the user’s private key will be used to sign a challenge and send it to the
   server, where it will be validated using the previously stored public key. Finally, if everything checks, the login
   completes and the welcome page will be displayed as before.

6. Passkey Repositories
   As mentioned before, the default passkey configuration created by Spring Security doesn’t provide persistence for the
   registered keys. To fix this, we need to provide implementations for the following interfaces:

PublicKeyCredentialUserEntityRepository
UserCredentialRepository
6.1. PublicKeyCredentialUserEntityRepository
This service manages PublicKeyCredentialUserEntity instances and maps user accounts managed by the standard
UserDetailsService to user account identifiers. This entity has the following attributes:

name: A user-friendly name identifier for the account
id: An opaque identifier for the user’s account
displayName: An alternate version of the account name, meant to be used for display purposes
It’s important to notice that the current implementation assumes that both name and id are unique within a given
authentication domain.

In general, we can assume that entries in this table have a 1:1 relationship with accounts managed by the standard
UserDetailsService.

The implementation, available online, uses Spring Data JDBC repositories to store those fields in the PASSKEY_USERS
table.

6.2. UserCredentialRepository
Manages CredentialRecord instances, which stores the actual public key received from the browser as part of the
registration process. This entity includes all the recommended properties specified in the W3C’s documentation, along
with some additional ones:

userEntityUserId: Identifier of the PublicKeyCredentialUserEntity that owns this credential
label: user-defined label for this credential, assigned at registration time
lastUsed: Date of last usage for this credential
created: Date of creation of this credential
Notice that CredentialRecord has an N:1 relationship with PublicKeyCredentialUserEntity, which reflects on the methods
of the repository. For instance, the findByUserId() method returns a list of CredentialRecord instances.

Our implementation takes this into account and uses a foreign key in the PASSKEY_CREDENTIALS table to ensure referential
integrity.

7. Testing
   While it is possible to test passkey-based applications using mock requests, the value of those tests is somewhat
   limited. Most failure scenarios are related to client-side issues, thus requiring integration tests that use a real
   browser driven by an automation tool.

   Here, we’ll use Selenium to implement a “happy path” scenario just to illustrate the technique. In particular, we’ll
   use the VirtualAuthenticator feature to configure the WebDriver, allowing us to simulate interactions between the
   registration and login page with this mechanism.

   For instance, this is how we can create a new driver with a VirtualAuthenticator:
    ```java
    @BeforeEach
    void setupTest() {
    VirtualAuthenticatorOptions options = new VirtualAuthenticatorOptions()
    .setIsUserVerified(true)
    .setIsUserConsenting(true)
    .setProtocol(VirtualAuthenticatorOptions.Protocol.CTAP2)
    .setHasUserVerification(true)
    .setHasResidentKey(true);
    
        driver = new ChromeDriver();
        authenticator = ((HasVirtualAuthenticator) driver).addVirtualAuthenticator(options);
    }
    ```
   Once we get the authenticator instance, we can use it to simulate different scenarios, such as a successful or
   unsuccessful login, registration, and so on. Our live test goes through a full cycle, consisting of the following
   steps:

   Initial login using username/password credentials
   Passkey registration
   Logout
   Login using the passkey
8. Conclusion
   In this tutorial, we’ve shown how to use Passkeys in a Spring Boot Web application, including the Spring Security
   setup and adding key persistence support needed for real-world applications.

   The code backing this article is available on GitHub. Once you're logged in as a Baeldung Pro Member, start learning
   and coding on the project.
   announcement - icon
   Connect with experts from the Java community, Microsoft, and partners to “Code the Future with AI” JDConf 2025, on
   April 9 - 10.

   Dedicated local streams across North America, Europe, and Asia-Pacific will explore the latest Java AI models to
   develop LLM apps and agents, learning best practices for app modernization with AI-assisted dev tools, learning the
   latest in Java frameworks, security, and quite a bit more:


