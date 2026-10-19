package com.privacy.config;

import com.privacy.entity.LearningModule;
import com.privacy.entity.QuizOption;
import com.privacy.entity.QuizQuestion;
import com.privacy.entity.User;
import com.privacy.repository.LearningModuleRepository;
import com.privacy.repository.QuizQuestionRepository;
import com.privacy.repository.UserRepository;
import com.privacy.entity.PhishingRedFlag;
import com.privacy.entity.PhishingScenario;
import com.privacy.repository.PhishingScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

/**
 * Seeds the database with an admin account and sample educational content
 * on first run. Safe to run multiple times Ã¢â‚¬â€ checks for existing data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final QuizQuestionRepository questionRepository;
    private final LearningModuleRepository learningModuleRepository;
    private final PhishingScenarioRepository scenarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedQuizQuestions();
        seedLearningModules();
        seedPhishingScenarios();
    }

    private void seedAdmin() {
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user '{}' already exists Ã¢â‚¬â€ skipping seed.", adminUsername);
            return;
        }
        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .fullName("System Administrator")
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Admin user '{}' created successfully.", adminUsername);
    }

    private void seedQuizQuestions() {
        if (questionRepository.count() > 0) {
            log.info("Quiz questions already seeded Ã¢â‚¬â€ skipping.");
            return;
        }
        log.info("Seeding quiz questions...");
        List<QuizQuestion> questions = List.of(
            buildQ("Which of the following is a common sign of a phishing email?",
                QuizQuestion.Category.PHISHING,
                "Phishing emails often create urgency and ask you to click links to verify your information. Legitimate companies rarely ask for sensitive details via email.",
                new String[]{"A generic greeting like 'Dear Customer' with urgent requests for account verification",
                             "A personalised email from your bank with your full name and last 4 digits of your card",
                             "A newsletter you subscribed to with an unsubscribe link",
                             "An order confirmation from a site you recently bought from"}, 0),

            buildQ("You receive a message claiming your social media account will be deleted unless you log in through a provided link. What should you do?",
                QuizQuestion.Category.PHISHING,
                "Always verify account alerts by navigating directly to the official website Ã¢â‚¬â€ never through links in unsolicited messages.",
                new String[]{"Log in through the provided link to protect your account",
                             "Ignore the message completely without checking",
                             "Navigate directly to the platform's official website to check your account status",
                             "Forward the message to your friends"}, 2),

            buildQ("Which password is the most secure?",
                QuizQuestion.Category.PASSWORD_SAFETY,
                "A long, random passphrase with mixed characters is far stronger than short passwords, even with symbols. Length is the single biggest factor in password strength.",
                new String[]{"password123",
                             "P@ss!",
                             "J7$mK#pL9qR!vB2n",
                             "MyName1990!"}, 2),

            buildQ("How often should you change your passwords for critical accounts?",
                QuizQuestion.Category.PASSWORD_SAFETY,
                "Best practice is to use a strong, unique password and change it immediately if a breach is suspected. Routine forced changes without breach evidence can lead to weaker passwords.",
                new String[]{"Never Ã¢â‚¬â€ once set is fine",
                             "Only when you suspect a breach or at regular intervals (e.g. annually)",
                             "Every day",
                             "Only when you forget it"}, 1),

            buildQ("What does 'oversharing' on social media mean in a privacy context?",
                QuizQuestion.Category.OVERSHARING,
                "Posting your location, daily routine, or personal details publicly allows strangers to build a detailed profile of you, enabling targeted scams and physical risks.",
                new String[]{"Sharing posts that are too long",
                             "Posting frequently throughout the day",
                             "Publicly sharing personal details like home address, daily routine, or location",
                             "Liking too many posts"}, 2),

            buildQ("You post a photo of your new home with the address visible in the background. What is the primary risk?",
                QuizQuestion.Category.OVERSHARING,
                "Publicly visible home addresses can be used by bad actors for targeted theft, stalking, or social engineering attacks.",
                new String[]{"Your photo quality looks poor",
                             "Strangers can identify and potentially misuse your home address",
                             "The post may get fewer likes",
                             "Your friends might visit unannounced"}, 1),

            buildQ("What is identity theft in the context of social media?",
                QuizQuestion.Category.IDENTITY_THEFT,
                "Identity thieves collect publicly available personal information from social media to impersonate you, access your accounts, or commit fraud in your name.",
                new String[]{"Someone borrowing your phone",
                             "Someone creating a fake profile with your name and photos to impersonate you or gather information",
                             "Posting your own achievements online",
                             "Forgetting your own password"}, 1),

            buildQ("Which piece of information is most dangerous to share publicly on social media?",
                QuizQuestion.Category.IDENTITY_THEFT,
                "Your date of birth combined with your full name is frequently used to pass security verification for financial accounts and government services.",
                new String[]{"Your favourite movie",
                             "Your full date of birth and hometown",
                             "Your preferred music genre",
                             "Your pet's name"}, 1),

            buildQ("What is cyberbullying?",
                QuizQuestion.Category.CYBERBULLYING,
                "Cyberbullying uses digital platforms to harass, humiliate, or intimidate individuals. It can have serious psychological effects and may be illegal.",
                new String[]{"Playing video games online against strangers",
                             "Sending spam emails about products",
                             "Using digital platforms to harass, threaten, or humiliate someone",
                             "Disagreeing with someone in a comment section"}, 2),

            buildQ("A classmate keeps posting embarrassing photos of you without permission. What is the appropriate first step?",
                QuizQuestion.Category.CYBERBULLYING,
                "Document the incident first to preserve evidence, then report through the platform and involve a trusted adult or authority if it continues.",
                new String[]{"Retaliate by posting embarrassing photos of them",
                             "Do nothing and hope it stops",
                             "Document the posts as evidence, report to the platform, and tell a trusted adult",
                             "Delete your social media account immediately"}, 2),

            buildQ("Which privacy setting is most important to review on a new social media account?",
                QuizQuestion.Category.PRIVACY_SETTINGS,
                "Profile visibility determines who can see your information. Setting this to private or friends-only is the single most effective way to limit exposure.",
                new String[]{"Notification preferences",
                             "Post visibility and profile audience control",
                             "Font size and theme",
                             "Language preference"}, 1),

            buildQ("Two-factor authentication (2FA) adds security by:",
                QuizQuestion.Category.PRIVACY_SETTINGS,
                "2FA ensures that even if your password is stolen, attackers cannot access your account without the second verification factor you control.",
                new String[]{"Making you log in twice with the same password",
                             "Requiring a second verification step (e.g. a code to your phone) in addition to your password",
                             "Emailing your password to you",
                             "Storing your password in a second location"}, 1),

            buildQ("What should you do before clicking any link in a social media direct message?",
                QuizQuestion.Category.PHISHING,
                "Hover over links to inspect the actual URL before clicking. Malicious links often use lookalike domains to steal credentials.",
                new String[]{"Click immediately to see where it goes",
                             "Verify the sender's identity and hover over the link to check the actual URL",
                             "Share it with your friends first",
                             "Ignore all messages with links"}, 1),

            buildQ("Using the same password across multiple social media accounts is risky because:",
                QuizQuestion.Category.PASSWORD_SAFETY,
                "Credential stuffing attacks use leaked username/password pairs from one breach to automatically access other services where you reused the same password.",
                new String[]{"It is harder to remember",
                             "If one account is breached, attackers can access all other accounts using the same password",
                             "Websites may penalise you for it",
                             "It takes longer to log in"}, 1),

            buildQ("Which of these is the safest approach to using public Wi-Fi on social media?",
                QuizQuestion.Category.PRIVACY_SETTINGS,
                "Public Wi-Fi is unencrypted and can be monitored. A VPN encrypts your traffic, preventing attackers from intercepting your credentials or session data.",
                new String[]{"Log in normally Ã¢â‚¬â€ public Wi-Fi is always secure",
                             "Only access non-sensitive sites on public Wi-Fi",
                             "Use a VPN to encrypt your connection before logging in",
                             "Share your Wi-Fi password with nearby people to verify trust"}, 2)
        );
        questionRepository.saveAll(questions);
        log.info("Seeded {} quiz questions.", questions.size());
    }

    private QuizQuestion buildQ(String text, QuizQuestion.Category category, String explanation,
                                 String[] options, int correctIndex) {
        QuizQuestion q = QuizQuestion.builder()
                .questionText(text)
                .category(category)
                .explanation(explanation)
                .points(10)
                .active(true)
                .build();

        for (int i = 0; i < options.length; i++) {
            QuizOption opt = QuizOption.builder()
                    .question(q)
                    .optionText(options[i])
                    .correct(i == correctIndex)
                    .displayOrder(i)
                    .build();
            q.getOptions().add(opt);
        }
        return q;
    }

    private void seedLearningModules() {
        if (learningModuleRepository.count() > 0) {
            log.info("Learning modules already seeded Ã¢â‚¬â€ skipping.");
            return;
        }
        log.info("Seeding learning modules...");

        List<LearningModule> modules = List.of(
            LearningModule.builder()
                .title("Understanding Phishing Attacks")
                .category(QuizQuestion.Category.PHISHING)
                .description("Learn how to recognise and avoid phishing scams targeting social media users.")
                .estimatedMinutes(8)
                .displayOrder(1)
                .content("""
                <h2>What is Phishing?</h2>
                <p>Phishing is a type of cyberattack where criminals impersonate trusted entities to trick you into revealing sensitive information such as passwords, credit card numbers, or personal details.</p>
                
                <h3>Common Phishing Signs on Social Media</h3>
                <ul>
                  <li><strong>Urgency:</strong> "Your account will be suspended in 24 hours!"</li>
                  <li><strong>Generic greetings:</strong> "Dear User" instead of your name</li>
                  <li><strong>Suspicious links:</strong> URLs that look similar but not identical to real sites (e.g., faceb00k.com)</li>
                  <li><strong>Requests for credentials:</strong> Legitimate platforms never ask for your password via message</li>
                  <li><strong>Too-good-to-be-true offers:</strong> Free gifts, prizes, or exclusive access</li>
                </ul>
                
                <h3>How to Protect Yourself</h3>
                <ol>
                  <li>Always navigate directly to websites Ã¢â‚¬â€ never through links in messages</li>
                  <li>Check the actual URL by hovering before clicking</li>
                  <li>Enable two-factor authentication on all social media accounts</li>
                  <li>Report suspicious messages to the platform</li>
                  <li>When in doubt, verify through official channels</li>
                </ol>
                
                <h3>Real-World Example</h3>
                <p>A user receives a message: <em>"Instagram Security Team: We detected suspicious login activity. Click here to verify your account: instagram-security-verify.com"</em></p>
                <p>This is a phishing attempt. The domain is not Instagram's real domain. Instagram only communicates through app notifications and @instagram.com email addresses.</p>
                """)
                .active(true)
                .build(),

            LearningModule.builder()
                .title("Password Safety and Management")
                .category(QuizQuestion.Category.PASSWORD_SAFETY)
                .description("Discover best practices for creating and managing strong, unique passwords.")
                .estimatedMinutes(6)
                .displayOrder(2)
                .content("""
                <h2>Why Password Safety Matters</h2>
                <p>Weak or reused passwords are one of the most common causes of account compromise. When attackers breach one site, they try the same credentials on other platforms Ã¢â‚¬â€ a technique called <strong>credential stuffing</strong>.</p>
                
                <h3>Characteristics of a Strong Password</h3>
                <ul>
                  <li>At least 12Ã¢â‚¬â€œ16 characters long</li>
                  <li>Mix of uppercase, lowercase, numbers, and special characters</li>
                  <li>Not based on personal information (name, birthday, pet's name)</li>
                  <li>Unique for every account</li>
                </ul>
                
                <h3>Password Manager Benefits</h3>
                <p>A password manager generates and stores unique, complex passwords for every account. You only need to remember one strong master password. Recommended tools include Bitwarden (free, open-source), 1Password, and KeePass.</p>
                
                <h3>Two-Factor Authentication (2FA)</h3>
                <p>Enable 2FA on every social media account. Even if your password is stolen, attackers cannot access your account without your second factor (authenticator app, SMS code, or hardware key).</p>
                
                <h3>What to Do If Your Password Is Compromised</h3>
                <ol>
                  <li>Change the password immediately on the affected site</li>
                  <li>Change the same password anywhere else you used it</li>
                  <li>Check haveibeenpwned.com to see if your email appears in known breaches</li>
                  <li>Enable 2FA if you have not already</li>
                  <li>Review active sessions and log out unknown devices</li>
                </ol>
                """)
                .active(true)
                .build(),

            LearningModule.builder()
                .title("Privacy Settings on Social Media")
                .category(QuizQuestion.Category.PRIVACY_SETTINGS)
                .description("Learn how to configure your privacy settings to control who sees your information.")
                .estimatedMinutes(7)
                .displayOrder(3)
                .content("""
                <h2>Taking Control of Your Privacy</h2>
                <p>Every major social media platform provides privacy settings that let you control who can see your content, contact you, and find your profile. These settings are often set to public by default.</p>
                
                <h3>Essential Settings to Review</h3>
                <ul>
                  <li><strong>Profile visibility:</strong> Set to Private or Friends Only</li>
                  <li><strong>Who can send you messages:</strong> Limit to friends or connections</li>
                  <li><strong>Tag approvals:</strong> Review tags before they appear on your profile</li>
                  <li><strong>Location data:</strong> Disable precise location in posts</li>
                  <li><strong>Third-party app access:</strong> Revoke permissions for unused apps</li>
                  <li><strong>Ad preferences:</strong> Limit data used for ad targeting</li>
                </ul>
                
                <h3>Active Sessions</h3>
                <p>Regularly check which devices and locations have active sessions on your account. If you see an unfamiliar device or location, terminate that session immediately and change your password.</p>
                
                <h3>Public Wi-Fi Risks</h3>
                <p>Avoid logging into social media on public Wi-Fi. If necessary, use a VPN to encrypt your connection. Attackers on the same network can intercept unencrypted traffic and capture your session cookies.</p>
                """)
                .active(true)
                .build(),

            LearningModule.builder()
                .title("Identity Theft Prevention")
                .category(QuizQuestion.Category.IDENTITY_THEFT)
                .description("Understand how identity thieves operate and how to protect your personal information.")
                .estimatedMinutes(9)
                .displayOrder(4)
                .content("""
                <h2>What is Identity Theft?</h2>
                <p>Identity theft occurs when someone uses your personal information without permission to commit fraud, access your accounts, or impersonate you online.</p>
                
                <h3>How Thieves Gather Information</h3>
                <ul>
                  <li>Scraping publicly visible social media profiles</li>
                  <li>Purchasing data from breached databases on the dark web</li>
                  <li>Social engineering Ã¢â‚¬â€ tricking you into revealing information</li>
                  <li>Fake quizzes and surveys that collect personal data</li>
                </ul>
                
                <h3>What Information is Risky to Share?</h3>
                <ul>
                  <li>Full name + date of birth + hometown (enough to pass security questions)</li>
                  <li>Phone number (enables SIM swapping attacks)</li>
                  <li>Home address or current location</li>
                  <li>Workplace name and schedule</li>
                  <li>Photos of ID documents, tickets, or boarding passes</li>
                </ul>
                
                <h3>Warning Signs of Identity Theft</h3>
                <ul>
                  <li>Unexpected account recovery emails you did not request</li>
                  <li>Friends reporting messages sent from your account that you did not send</li>
                  <li>Unfamiliar logins in your account security history</li>
                  <li>Fake profiles using your name or photos</li>
                </ul>
                
                <h3>What to Do If Your Identity is Stolen</h3>
                <ol>
                  <li>Secure all accounts by changing passwords and enabling 2FA</li>
                  <li>Report fake profiles to the platform</li>
                  <li>File a complaint with cybercrime authorities</li>
                  <li>Notify your bank if financial information may be involved</li>
                  <li>Document all evidence</li>
                </ol>
                """)
                .active(true)
                .build(),

            LearningModule.builder()
                .title("Cyberbullying Awareness and Response")
                .category(QuizQuestion.Category.CYBERBULLYING)
                .description("Recognise cyberbullying, understand its impact, and learn how to respond effectively.")
                .estimatedMinutes(7)
                .displayOrder(5)
                .content("""
                <h2>Understanding Cyberbullying</h2>
                <p>Cyberbullying is the repeated use of digital technology to harass, threaten, embarrass, or target another person. Unlike traditional bullying, it can follow victims everywhere and occur at any time.</p>
                
                <h3>Forms of Cyberbullying</h3>
                <ul>
                  <li><strong>Harassment:</strong> Sending repeated offensive messages</li>
                  <li><strong>Flaming:</strong> Online arguments using hostile language</li>
                  <li><strong>Exclusion:</strong> Deliberately excluding someone from online groups</li>
                  <li><strong>Impersonation:</strong> Creating fake profiles to damage reputation</li>
                  <li><strong>Outing:</strong> Sharing someone's private information publicly</li>
                  <li><strong>Doxxing:</strong> Publishing someone's private details maliciously</li>
                </ul>
                
                <h3>If You Are Being Cyberbullied</h3>
                <ol>
                  <li><strong>Do not respond</strong> Ã¢â‚¬â€ engaging often escalates the situation</li>
                  <li><strong>Document everything</strong> Ã¢â‚¬â€ take screenshots with timestamps</li>
                  <li><strong>Block the person</strong> on all platforms</li>
                  <li><strong>Report</strong> to the platform using built-in reporting tools</li>
                  <li><strong>Tell a trusted adult</strong> Ã¢â‚¬â€ parent, teacher, or counsellor</li>
                  <li><strong>Contact authorities</strong> if threats involve physical harm</li>
                </ol>
                
                <h3>Supporting Someone Who is Cyberbullied</h3>
                <p>Don't share, like, or comment on bullying content Ã¢â‚¬â€ this amplifies it. Reach out privately to offer support. Encourage the person to report and document the incidents.</p>
                """)
                .active(true)
                .build(),

            LearningModule.builder()
                .title("Oversharing and Its Dangers")
                .category(QuizQuestion.Category.OVERSHARING)
                .description("Learn what oversharing is, why it is risky, and how to share safely online.")
                .estimatedMinutes(5)
                .displayOrder(6)
                .content("""
                <h2>What is Oversharing?</h2>
                <p>Oversharing means posting more personal information than is safe or necessary on social media. Even innocent-seeming details can combine to create a detailed picture of your life that bad actors can exploit.</p>
                
                <h3>The Aggregation Problem</h3>
                <p>No single piece of information may seem sensitive, but combining them creates risk:</p>
                <ul>
                  <li>Name + Employer + Neighbourhood + Daily Routine = Easy target for scams, theft, or stalking</li>
                  <li>Pet name + Birthday + Favourite sports team = Answers to common security questions</li>
                </ul>
                
                <h3>Common Oversharing Mistakes</h3>
                <ul>
                  <li>Posting holiday photos while you are still away (signals empty home)</li>
                  <li>Sharing boarding passes or event tickets (contains scannable codes)</li>
                  <li>Posting your exact location in real time</li>
                  <li>Sharing photos of your home's interior (valuables visible)</li>
                  <li>Venting about work frustrations with identifying details</li>
                </ul>
                
                <h3>Safe Sharing Principles</h3>
                <ol>
                  <li>Pause before posting Ã¢â‚¬â€ ask: "Would I be comfortable with a stranger seeing this?"</li>
                  <li>Set posts to friends-only rather than public</li>
                  <li>Delay location-based posts until after you have left</li>
                  <li>Strip metadata (GPS coordinates) from photos before sharing</li>
                  <li>Review your profile periodically and remove old sensitive information</li>
                </ol>
                """)
                .active(true)
                .build()
        );
        learningModuleRepository.saveAll(modules);
        log.info("Seeded {} learning modules.", modules.size());
    }

    private void seedPhishingScenarios() {
        if (scenarioRepository.count() > 0) {
            log.info("Phishing scenarios already seeded â€” skipping.");
            return;
        }
        log.info("Seeding 10 fictional phishing scenarios...");

        List<PhishingScenario> scenarios = new ArrayList<>();

        // 1. ApexBank Unauthorized Transfer (Email)
        PhishingScenario s1 = PhishingScenario.builder()
                .title("ApexBank: Unauthorized Wire Transfer Detected")
                .channel(PhishingScenario.Channel.EMAIL)
                .sender("security-alert@apexbank-notice.net")
                .subject("URGENT: Suspicious transfer of $4,850.00 initiated from new device")
                .content("""
                <div style="font-family:sans-serif;color:#333;line-height:1.6">
                  <h3 style="color:#c53030">ApexBank Security Alert</h3>
                  <p>Dear Valued Customer,</p>
                  <p>Our automated fraud monitoring system detected an unauthorized wire transfer attempt of <strong>$4,850.00 USD</strong> to an offshore recipient.</p>
                  <p><strong>Device:</strong> Unknown Linux Client (IP: 185.220.101.5)<br>
                  <strong>Time:</strong> Today at 03:14 AM EST</p>
                  <p>If you did not authorize this transaction, you must cancel it immediately within <strong>15 minutes</strong> to prevent irreversible fund settlement.</p>
                  <p style="margin:20px 0">
                    <a href="http://apexbank-secure-cancel.net/login?case=9482" style="background:#c53030;color:#fff;padding:12px 20px;text-decoration:none;border-radius:6px;font-weight:bold;display:inline-block">Cancel Wire Transfer Immediately</a>
                  </p>
                  <p style="font-size:12px;color:#718096">ApexBank National Association. Member FDIC.</p>
                </div>
                """)
                .difficulty(PhishingScenario.Difficulty.BEGINNER)
                .explanation("Classic high-pressure banking lure. Legitimate institutions never use third-party domains like apexbank-notice.net or apexbank-secure-cancel.net. Notice the generic greeting ('Dear Valued Customer') and an artificial 15-minute countdown designed to induce panic.")
                .active(true)
                .build();

        s1.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s1).flagText("Spoofed sender domain 'apexbank-notice.net' instead of official bank domain").description("Banks always use their registered primary domain name for official correspondence.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s1).flagText("Extreme artificial urgency (15-minute countdown to trigger panic)").description("Urgency prevents the victim from independently verifying with the bank through official channels.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s1).flagText("Deceptive hyperlink pointing to 'apexbank-secure-cancel.net'").description("The link directs to an external harvesting page mimicking the bank's portal.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s1).flagText("Generic greeting ('Dear Valued Customer') instead of user's legal name").description("Phishing campaigns mass-email thousands of addresses without customer names.").correct(true).displayOrder(4).build(),
                PhishingRedFlag.builder().scenario(s1).flagText("The email contains the FDIC member disclaimer").description("Distractor: Scammers frequently copy authentic disclaimers from real websites to appear legitimate.").correct(false).displayOrder(5).build()
        ));
        scenarios.add(s1);

        // 2. NetStream Membership Suspended (SMS)
        PhishingScenario s2 = PhishingScenario.builder()
                .title("NetStream: Streaming Membership Suspension")
                .channel(PhishingScenario.Channel.SMS)
                .sender("+1-888-555-0199")
                .subject(null)
                .content("NetStream: Your streaming membership is suspended due to a payment processing error. Update your billing card now to avoid account deletion: http://netstream-billing-update.info/pay")
                .difficulty(PhishingScenario.Difficulty.BEGINNER)
                .explanation("Smishing (SMS phishing) attacking subscription services. Uses an obscure .info domain and threatens permanent account cancellation.")
                .active(true)
                .build();

        s2.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s2).flagText("Unsolicited SMS with an unverified external link (.info TLD)").description("Subscription services instruct you to log into their verified app rather than clicking random SMS URLs.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s2).flagText("Immediate threat of account deletion to compel hasty card entry").description("Threat of loss causes people to bypass standard verification checks.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s2).flagText("The message originates from a toll-free number").description("Distractor: Scammers easily rent or spoof 888 and 800 numbers via cheap VOIP services.").correct(false).displayOrder(3).build()
        ));
        scenarios.add(s2);

        // 3. InstaVibe Blue Badge Verification (DM)
        PhishingScenario s3 = PhishingScenario.builder()
                .title("InstaVibe: Official Blue Badge Verification Offer")
                .channel(PhishingScenario.Channel.DIRECT_MESSAGE)
                .sender("@instavibe_official_support_v2")
                .subject(null)
                .content("""
                Hey there! ðŸŽ‰ Congratulations!

                Your profile has been selected by our moderation team for the Official Verified Blue Badge. Because of your rapid engagement growth, you qualify without subscriber requirements.

                Please complete the identity verification badge form here within 12 hours:
                ðŸ‘‰ http://instavibe-badge-verify.biz/apply

                Failure to respond will revoke badge eligibility.
                """)
                .difficulty(PhishingScenario.Difficulty.BEGINNER)
                .explanation("Social media credential harvester. Major social platforms NEVER initiate verification via Direct Messages. The handle contains suspicious underscores and '_v2', linking to an external .biz site.")
                .active(true)
                .build();

        s3.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s3).flagText("Social media platform offering badge verification via private DM").description("Legitimate social networks strictly manage badge verification inside official account settings, never DMs.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s3).flagText("Suspicious account handle with suffix '@instavibe_official_support_v2'").description("Impersonation handles use terms like 'official', 'support', or 'v2' to simulate authority.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s3).flagText("Deceptive third-party domain 'instavibe-badge-verify.biz'").description("Harvests social media login credentials, 2FA backup codes, or email passwords.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s3).flagText("The message praises your profile engagement growth").description("Distractor: Flattery is a classic social engineering hook to disarm critical skepticism.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s3);

        // 4. QuickCourier Outstanding Customs Fee (Email)
        PhishingScenario s4 = PhishingScenario.builder()
                .title("QuickCourier: Package Clearance Fee Pending")
                .channel(PhishingScenario.Channel.EMAIL)
                .sender("tracking-dispatch@quickcourier-express.com")
                .subject("Delivery Exception: Outstanding clearance fee ($2.85) for package #QC-83921")
                .content("""
                <div style="font-family:sans-serif;color:#333;line-height:1.6">
                  <div style="border-bottom:2px solid #2b6cb0;padding-bottom:10px;margin-bottom:15px">
                    <h2 style="color:#2b6cb0;margin:0">QuickCourier Express</h2>
                  </div>
                  <p>Hello,</p>
                  <p>Your package with tracking identifier <strong>#QC-83921-US</strong> arrived at our central distribution hub but has an outstanding customs duty and inspection surcharge of <strong>$2.85 USD</strong>.</p>
                  <p>To release the parcel for final local delivery tomorrow morning, please settle the outstanding balance:</p>
                  <p style="margin:20px 0">
                    <a href="http://quickcourier-pay-fees.org/tracking?id=83921" style="background:#2b6cb0;color:#fff;padding:10px 18px;text-decoration:none;border-radius:4px;font-weight:bold;display:inline-block">Pay $2.85 & Release Package</a>
                  </p>
                  <p style="font-size:12px;color:#718096">If unpaid within 48 hours, the parcel will be declared abandoned and returned to sender.</p>
                </div>
                """)
                .difficulty(PhishingScenario.Difficulty.INTERMEDIATE)
                .explanation("The 'Micro-Payment Parcel Scams' leverage tiny dollar amounts ($2.85) so victims assume it's harmless. The true objective is harvesting credit card numbers, CVVs, and billing addresses.")
                .active(true)
                .build();

        s4.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s4).flagText("Unsolicited delivery notification with a micro-fee ($2.85) to harvest card data").description("Low amounts disguise credit card theft as trivial routine charges.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s4).flagText("External payment gateway domain 'quickcourier-pay-fees.org'").description("Legitimate shipping couriers keep payments on their verified primary domain.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s4).flagText("Missing recipient name and delivery address details").description("Automated mass-phishing blasts send identical tracking numbers to thousands.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s4).flagText("The email specifies an exact tracking number format (#QC-83921-US)").description("Distractor: Scammers invent realistic tracking IDs to establish false credibility.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s4);

        // 5. GlobalTel Loyalty Points Expiring (SMS)
        PhishingScenario s5 = PhishingScenario.builder()
                .title("GlobalTel: Expiring Loyalty Points Incentive")
                .channel(PhishingScenario.Channel.SMS)
                .sender("+44-7700-900821")
                .subject(null)
                .content("GlobalTel Alert: You have 8,450 loyalty reward points ($150 value) expiring tonight at 23:59! Redeem now for electronic vouchers or devices: http://globaltel-points-portal.net/redeem")
                .difficulty(PhishingScenario.Difficulty.BEGINNER)
                .explanation("Greed and urgency combination smishing lure. Threatens loss of imaginary money to prompt credential entry.")
                .active(true)
                .build();

        s5.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s5).flagText("Artificial midnight deadline to compel unverified action").description("Creating urgency stops the victim from verifying whether they even participate in the loyalty program.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s5).flagText("Phishing link 'globaltel-points-portal.net' instead of official carrier site").description("Scammers construct hyphenated domains to mimic legitimate service names.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s5).flagText("Unsolicited financial prize or reward value notification").description("Any unearned financial reward message must be treated with immediate skepticism.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s5).flagText("The SMS mentions a specific point amount (8,450)").description("Distractor: Specific numbers make fake promotions sound authentic.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s5);

        // 6. FaceConnect Copyright Infringement Takedown (DM)
        PhishingScenario s6 = PhishingScenario.builder()
                .title("FaceConnect: Copyright Violation Warning")
                .channel(PhishingScenario.Channel.DIRECT_MESSAGE)
                .sender("@copyright_enforcement_division")
                .subject(null)
                .content("""
                âš ï¸ OFFICIAL NOTICE: Intellectual Property Infringement

                A DMCA copyright complaint has been lodged against photos uploaded to your account.
                Case Reference: #CP-98124

                If you do not submit a counter-dispute within 24 hours, your account and associated pages will be permanently disabled per our terms of service.

                Review complaint & file appeal:
                ðŸ”— http://faceconnect-dispute-resolution.cc/appeal?ref=98124
                """)
                .difficulty(PhishingScenario.Difficulty.INTERMEDIATE)
                .explanation("Exploits fear of losing social accounts and followers. Social platforms deliver intellectual property notices via formal in-app compliance alerts, not informal direct messages with foreign .cc links.")
                .active(true)
                .build();

        s6.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s6).flagText("Official DMCA complaint sent as a casual direct message").description("True copyright strikes appear in the account support inbox/dashboard, not via chat.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s6).flagText("Threat of permanent account termination within 24 hours").description("Severe consequences with short response windows are classic coercive social engineering.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s6).flagText("Deceptive domain with country-code TLD 'faceconnect-dispute-resolution.cc'").description("Using .cc (Cocos Islands) or obscure ccTLDs is a hallmark of disposable phishing hosts.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s6).flagText("The notice references a formal Case Reference number (#CP-98124)").description("Distractor: Fake case numbers are generated by attack kits to mimic legal rigor.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s6);

        // 7. CloudDrive Storage Limit Exceeded (Email)
        PhishingScenario s7 = PhishingScenario.builder()
                .title("CloudDrive: Storage Quota Exceeded & Deletion Notice")
                .channel(PhishingScenario.Channel.EMAIL)
                .sender("system@clouddrive-quota-warning.net")
                .subject("WARNING: Your CloudDrive is at 104% capacity â€” Deletion scheduled")
                .content("""
                <div style="font-family:sans-serif;color:#333;line-height:1.6">
                  <h3>CloudDrive Quota Notification</h3>
                  <p>Your cloud storage has reached <strong>15.6 GB of 15.0 GB</strong> (104% utilization).</p>
                  <p>Incoming syncs, shared albums, and email attachments have been temporarily paused.</p>
                  <p style="color:#d9534f"><strong>Warning:</strong> Accounts over quota for more than 48 hours will have older files permanently purged to free space.</p>
                  <p style="margin:20px 0">
                    <a href="http://clouddrive-storage-upgrade.com/auth" style="background:#0275d8;color:#fff;padding:10px 18px;text-decoration:none;border-radius:4px;font-weight:bold;display:inline-block">Claim Free +25 GB Promotional Upgrade</a>
                  </p>
                  <p style="font-size:12px;color:#777">CloudDrive Global Corporation.</p>
                </div>
                """)
                .difficulty(PhishingScenario.Difficulty.INTERMEDIATE)
                .explanation("Cloud storage credential harvesting. Targets users who fear losing photos or documents. Entices victim with a 'Free +25 GB' upgrade link on an unauthorized domain.")
                .active(true)
                .build();

        s7.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s7).flagText("Mismatched sender domain 'clouddrive-quota-warning.net'").description("Genuine cloud providers email from their official corporate domain.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s7).flagText("Threat of permanent file purging within 48 hours").description("Legitimate services freeze uploads rather than immediately deleting customer data.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s7).flagText("External authentication link pointing to 'clouddrive-storage-upgrade.com/auth'").description("Designed to harvest cloud storage master logins and single-sign-on credentials.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s7).flagText("The email accurately specifies a 15 GB quota threshold").description("Distractor: Scammers quote standard free tier sizes (15 GB) to sound plausible.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s7);

        // 8. CryptoVault Airdrop Claim (DM)
        PhishingScenario s8 = PhishingScenario.builder()
                .title("CryptoVault: Unclaimed Community Airdrop")
                .channel(PhishingScenario.Channel.DIRECT_MESSAGE)
                .sender("@cryptovault_community_bot")
                .subject(null)
                .content("""
                ðŸŽ Congratulations Active Member!

                You have been randomly selected for the Season 2 Governance Airdrop: 500 C-TOKEN ($750.00 USD value).

                To claim before the token claim contract closes:
                1. Visit: http://cryptovault-airdrop-claim.xyz/connect
                2. Connect your wallet extension
                3. Input your 12-word Secret Recovery Phrase to verify on-chain ownership
                4. Tokens will deposit instantly to your address.
                """)
                .difficulty(PhishingScenario.Difficulty.ADVANCED)
                .explanation("Crypto wallet drainer attack. Rule #1 of Web3 security: NEVER disclose your 12/24-word Secret Recovery Phrase or private keys to ANY website, bot, or admin. Anyone with your seed phrase owns 100% of your funds.")
                .active(true)
                .build();

        s8.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s8).flagText("Direct request for your 12-word Secret Recovery Phrase / private keys").description("Legitimate crypto applications NEVER ask for your seed phrase. Doing so drains all assets.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s8).flagText("Unsolicited high-value financial token airdrop ($750 USD)").description("Free money lures are the #1 attack vector in decentralized finance theft.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s8).flagText("High-risk phishing domain using '.xyz' TLD").description("Attackers favor cheap, anonymous TLDs like .xyz for short-lived drainer sites.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s8).flagText("The message mentions smart contracts and governance tokens").description("Distractor: Using cryptocurrency technical jargon creates a false impression of legitimacy.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s8);

        // 9. SecurePay Fraudulent Foreign Login (SMS)
        PhishingScenario s9 = PhishingScenario.builder()
                .title("SecurePay: Fraudulent Login Lockout Alert")
                .channel(PhishingScenario.Channel.SMS)
                .sender("SECURE-PAY")
                .subject(null)
                .content("SecurePay: Security Alert! New sign-in detected from Paris, France. If this was not you, secure your account immediately: http://securepay-safety-center.info/unlock?id=892")
                .difficulty(PhishingScenario.Difficulty.BEGINNER)
                .explanation("Smishing using geographic anomaly fear. By naming a foreign city, it triggers instant alarm so the recipient clicks without verifying.")
                .active(true)
                .build();

        s9.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s9).flagText("Geographic scare tactic (unrecognized foreign login location)").description("Surprise locations induce fear, compelling the victim to react impulsively.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s9).flagText("Deceptive domain 'securepay-safety-center.info'").description("Mimics security portals but registers on cheap .info infrastructure to harvest passwords & 2FA.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s9).flagText("SMS lacks official sender shortcode or personalized customer identifier").description("Official financial alerts display verified shortcodes or include masked account digits.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s9).flagText("The sender displays the company name 'SECURE-PAY' in the caller ID").description("Distractor: Sender alphanumeric IDs can be spoofed in many international SMS gateways.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s9);

        // 10. GlobalHR Workplace Policy Acknowledgment (Email)
        PhishingScenario s10 = PhishingScenario.builder()
                .title("GlobalHR: Mandatory Workplace Policy Sign-off")
                .channel(PhishingScenario.Channel.EMAIL)
                .sender("humanresources@globalhr-enterprise-portal.biz")
                .subject("MANDATORY: 2026 Employee Workplace Policy Acknowledgment (Due Today)")
                .content("""
                <div style="font-family:sans-serif;color:#333;line-height:1.6">
                  <p>All Staff,</p>
                  <p>In accordance with updated annual compliance standards, all active personnel are required to review and electronically sign the <strong>2026 Workplace Conduct & Privacy Policy</strong> by end of business today (5:00 PM).</p>
                  <p>Failure to complete this mandatory sign-off may result in suspension of enterprise intranet credentials and delayed payroll processing.</p>
                  <p style="margin:20px 0">
                    <a href="http://globalhr-enterprise-portal.biz/sso-login" style="background:#28a745;color:#fff;padding:10px 18px;text-decoration:none;border-radius:4px;font-weight:bold;display:inline-block">Authenticate with SSO & Sign Policy</a>
                  </p>
                  <p style="font-size:12px;color:#666">GlobalHR Enterprise Solutions â€” Human Resources Compliance Bureau.</p>
                </div>
                """)
                .difficulty(PhishingScenario.Difficulty.ADVANCED)
                .explanation("Corporate spear-phishing (Business Email Compromise). Uses workplace compliance and threat of payroll delay to harvest corporate Single-Sign-On (SSO) credentials.")
                .active(true)
                .build();

        s10.getRedFlags().addAll(List.of(
                PhishingRedFlag.builder().scenario(s10).flagText("Threat of HR disciplinary action or payroll disruption").description("Pretexting HR authority with punitive threats coerces rapid employee submission.").correct(true).displayOrder(1).build(),
                PhishingRedFlag.builder().scenario(s10).flagText("External domain 'globalhr-enterprise-portal.biz' mimicking internal corporate SSO").description("Corporate intranets reside on internal domain names, not external public .biz websites.").correct(true).displayOrder(2).build(),
                PhishingRedFlag.builder().scenario(s10).flagText("Same-day urgent deadline ('Due Today 5:00 PM')").description("Imposing severe time limits prevents employees from consulting their manager or IT security.").correct(true).displayOrder(3).build(),
                PhishingRedFlag.builder().scenario(s10).flagText("The email references standard annual compliance policy review").description("Distractor: Using familiar corporate routines masks the fraudulent intent.").correct(false).displayOrder(4).build()
        ));
        scenarios.add(s10);

        scenarioRepository.saveAll(scenarios);
        log.info("Seeded {} phishing scenarios with detailed red flag indicators.", scenarios.size());
    }
}
