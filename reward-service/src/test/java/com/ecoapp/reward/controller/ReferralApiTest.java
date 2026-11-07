package com.ecoapp.reward.controller;

import com.ecoapp.reward.exception.GlobalExceptionHandler;
import com.ecoapp.reward.model.Referral;
import com.ecoapp.reward.model.ReferralCode;
import com.ecoapp.reward.model.ReferralStatus;
import com.ecoapp.reward.repository.BadgeRepository;
import com.ecoapp.reward.repository.DepositRecordRepository;
import com.ecoapp.reward.repository.ReferralCodeRepository;
import com.ecoapp.reward.repository.ReferralRepository;
import com.ecoapp.reward.repository.UserBadgeRepository;
import com.ecoapp.reward.repository.UserStatsRepository;
import com.ecoapp.reward.security.RequireUserIdFilter;
import com.ecoapp.reward.service.BadgeService;
import com.ecoapp.reward.service.ReferralService;
import com.ecoapp.reward.service.RewardService;
import com.ecoapp.reward.service.StreakService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API-level tests for the referral endpoints and the deposit flow.
 * The web layer is real (controllers, Jackson, GlobalExceptionHandler, RequireUserIdFilter) and so are
 * ReferralService and RewardService; only repositories and the badge/streak collaborators are mocked.
 * Every test gets fresh mocks, so no state leaks between tests.
 */
class ReferralApiTest {

    private static final String VALID_CODE = "K7M2QX9P";
    private static final String GENERIC_INVALID_BODY =
            "{\"error\":\"INVALID_REFERRAL\",\"message\":\"Referral could not be claimed\"}";
    private static final String GENERIC_UNAUTHORIZED_BODY =
            "{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing verified identity\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ReferralCodeRepository codeRepo = mock(ReferralCodeRepository.class);
    private final ReferralRepository referralRepo = mock(ReferralRepository.class);
    private final UserStatsRepository statsRepo = mock(UserStatsRepository.class);
    private final DepositRecordRepository depositRepo = mock(DepositRecordRepository.class);
    private final UserBadgeRepository userBadgeRepo = mock(UserBadgeRepository.class);
    private final BadgeRepository badgeRepo = mock(BadgeRepository.class);
    private final BadgeService badgeService = mock(BadgeService.class);
    private final StreakService streakService = mock(StreakService.class);

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC);

    private final ReferralService referralService = new ReferralService(
            codeRepo, referralRepo, statsRepo, depositRepo, userBadgeRepo, badgeService, clock);
    private final com.ecoapp.reward.client.ScanDepositClient scanClient =
            mock(com.ecoapp.reward.client.ScanDepositClient.class);
    private final RewardService rewardService = new RewardService(
            statsRepo, badgeRepo, userBadgeRepo, depositRepo, badgeService, streakService, referralService,
            scanClient, new org.springframework.transaction.support.TransactionTemplate(
                    mock(org.springframework.transaction.PlatformTransactionManager.class)));

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(scanClient.check(any())).thenReturn(new com.ecoapp.reward.client.ScanDepositClient.DepositCheck(true, 0.15));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReferralController(referralService), new RewardController(rewardService, streakService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequireUserIdFilter())
                .build();
    }

    // ---------------------------------------------------------------- GET /api/rewards/referrals/me

    @Test
    void shouldReturn200WithCodeAndCountsWhenGettingOwnSummary() throws Exception {
        stubOwnCode("user-1", VALID_CODE);
        when(referralRepo.countByReferrerUserIdAndStatus("user-1", ReferralStatus.CREDITED)).thenReturn(2L);
        when(referralRepo.countByReferrerUserIdAndStatus("user-1", ReferralStatus.PENDING)).thenReturn(1L);

        mockMvc.perform(get("/api/rewards/referrals/me").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(VALID_CODE))
                .andExpect(jsonPath("$.creditedCount").value(2))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.ambassadorUnlocked").value(false));
    }

    @Test
    void shouldReturnAmbassadorUnlockedTrueWhenBadgeIsEarned() throws Exception {
        stubOwnCode("user-1", VALID_CODE);
        when(userBadgeRepo.findBadgeIdsByUserId("user-1")).thenReturn(Set.of(ReferralService.AMBASSADOR_BADGE_ID));

        mockMvc.perform(get("/api/rewards/referrals/me").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambassadorUnlocked").value(true));
    }

    @Test
    void shouldReturnOnlyTheFourSummaryFieldsWhenGettingOwnSummary() throws Exception {
        stubOwnCode("user-1", VALID_CODE);
        // A pending referral from another user exists in the data; its identity must not leak.
        when(referralRepo.findByInvitedUserIdAndStatus("user-1", ReferralStatus.PENDING))
                .thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(get("/api/rewards/referrals/me").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(List.of("code", "creditedCount", "pendingCount", "ambassadorUnlocked"), fieldNames(body));
        assertFalse(result.getResponse().getContentAsString().contains("user-1"));
    }

    @Test
    void shouldCountOnlyTheCallersReferralsWhenGettingOwnSummary() throws Exception {
        stubOwnCode("user-1", VALID_CODE);

        mockMvc.perform(get("/api/rewards/referrals/me").header("X-User-Id", "user-1"))
                .andExpect(status().isOk());

        verify(referralRepo).countByReferrerUserIdAndStatus("user-1", ReferralStatus.CREDITED);
        verify(referralRepo).countByReferrerUserIdAndStatus("user-1", ReferralStatus.PENDING);
        verify(referralRepo, never()).countByReferrerUserIdAndStatus(eq("other-user"), any());
    }

    @Test
    void shouldReturn401WithGenericBodyWhenSummaryHasNoUserIdHeader() throws Exception {
        mockMvc.perform(get("/api/rewards/referrals/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(GENERIC_UNAUTHORIZED_BODY));
    }

    @Test
    void shouldReturn401WhenSummaryUserIdHeaderIsBlank() throws Exception {
        mockMvc.perform(get("/api/rewards/referrals/me").header("X-User-Id", "   "))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------- POST /api/rewards/referrals/claim

    @Test
    void shouldReturn200WithClaimedTrueWhenCodeIsValid() throws Exception {
        stubCode(VALID_CODE, "referrer-1");
        when(referralRepo.insertPendingIfAbsent(eq("invitee-1"), eq("referrer-1"), any())).thenReturn(1);

        mockMvc.perform(claim("invitee-1", claimBody(VALID_CODE)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"claimed\":true}"));
    }

    @Test
    void shouldReturn400WithGenericBodyWhenCodeIsUnknown() throws Exception {
        when(codeRepo.findByCode("ZZZZZZZZ")).thenReturn(Optional.empty());

        mockMvc.perform(claim("invitee-1", claimBody("ZZZZZZZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(GENERIC_INVALID_BODY));
    }

    @Test
    void shouldReturn400WithGenericBodyWhenClaimingOwnCode() throws Exception {
        stubCode(VALID_CODE, "user-1");

        mockMvc.perform(claim("user-1", claimBody(VALID_CODE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(GENERIC_INVALID_BODY));
    }

    @Test
    void shouldReturn400WithGenericBodyWhenCodeIsClaimedAgain() throws Exception {
        stubCode(VALID_CODE, "referrer-1");
        when(referralRepo.insertPendingIfAbsent(eq("invitee-1"), eq("referrer-1"), any())).thenReturn(0);

        mockMvc.perform(claim("invitee-1", claimBody(VALID_CODE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(GENERIC_INVALID_BODY));
    }

    @Test
    void shouldReturn400WithGenericBodyWhenClaimingAfterAFirstDeposit() throws Exception {
        stubCode(VALID_CODE, "referrer-1");
        when(depositRepo.countByUserId("invitee-1")).thenReturn(1L);

        mockMvc.perform(claim("invitee-1", claimBody(VALID_CODE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(GENERIC_INVALID_BODY));
        verify(referralRepo, never()).insertPendingIfAbsent(any(), any(), any());
    }

    @Test
    void shouldReturnIdenticalErrorBodyForEveryRejectedClaimReason() throws Exception {
        String unknown = mockMvc.perform(claimAfterUnknownCode())
                .andReturn().getResponse().getContentAsString();
        String own = mockMvc.perform(claimOwnCode())
                .andReturn().getResponse().getContentAsString();
        String repeat = mockMvc.perform(claimRepeat())
                .andReturn().getResponse().getContentAsString();
        String afterDeposit = mockMvc.perform(claimAfterDeposit())
                .andReturn().getResponse().getContentAsString();

        // Compare parsed trees: Map.of does not guarantee key order in the serialized string.
        JsonNode expected = objectMapper.readTree(GENERIC_INVALID_BODY);
        assertEquals(expected, objectMapper.readTree(unknown));
        assertEquals(expected, objectMapper.readTree(own));
        assertEquals(expected, objectMapper.readTree(repeat));
        assertEquals(expected, objectMapper.readTree(afterDeposit));
    }

    @Test
    void shouldReturn400InvalidRequestAndNotLookUpCodeWhenCodeIsTooShort() throws Exception {
        mockMvc.perform(claim("invitee-1", claimBody("K7M2QX9")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        verifyNoInteractions(codeRepo);
    }

    @Test
    void shouldReturn400InvalidRequestAndNotLookUpCodeWhenCodeIsLowercase() throws Exception {
        mockMvc.perform(claim("invitee-1", claimBody("k7m2qx9p")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        verifyNoInteractions(codeRepo);
    }

    @Test
    void shouldReturn400InvalidRequestAndNotLookUpCodeWhenCodeContainsSymbols() throws Exception {
        mockMvc.perform(claim("invitee-1", claimBody("K7M2QX9'")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        verifyNoInteractions(codeRepo);
    }

    @Test
    void shouldReturn400InvalidRequestAndNotLookUpCodeWhenCodeContainsAmbiguousZero() throws Exception {
        mockMvc.perform(claim("invitee-1", claimBody("K7M2QX90")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        verifyNoInteractions(codeRepo);
    }

    @Test
    void shouldReturn400InvalidRequestWhenCodeFieldIsMissing() throws Exception {
        mockMvc.perform(claim("invitee-1", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        verifyNoInteractions(codeRepo);
    }

    @Test
    void shouldReturn400WhenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(claim("invitee-1", "{\"code\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        verifyNoInteractions(codeRepo);
    }

    @Test
    void shouldReturn400WhenRequestBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/rewards/referrals/claim")
                        .header("X-User-Id", "invitee-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(codeRepo);
    }

    @Test
    void shouldReturn400WhenBodyHasUnknownExtraField() throws Exception {
        // The code is valid and claimable, so only the extra field can cause a 400 here.
        stubCode(VALID_CODE, "referrer-1");
        when(referralRepo.insertPendingIfAbsent(eq("invitee-1"), eq("referrer-1"), any())).thenReturn(1);

        mockMvc.perform(claim("invitee-1", "{\"code\":\"" + VALID_CODE + "\",\"userId\":\"referrer-1\"}"))
                .andExpect(status().isBadRequest());
        verify(referralRepo, never()).insertPendingIfAbsent(any(), any(), any());
    }

    @Test
    void shouldReturn401AndNotLookUpCodeWhenClaimHasNoUserIdHeader() throws Exception {
        mockMvc.perform(post("/api/rewards/referrals/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody(VALID_CODE)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(GENERIC_UNAUTHORIZED_BODY));
        verifyNoInteractions(codeRepo);
    }

    // ------------------------------------------------------------------ error body hygiene

    @Test
    void shouldNotExposeStackTraceOrExceptionTextInRejectedClaimBody() throws Exception {
        when(codeRepo.findByCode("ZZZZZZZZ")).thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(claim("invitee-1", claimBody("ZZZZZZZZ")))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertNoStackTraceOrInternals(result.getResponse().getContentAsString());
    }

    @Test
    void shouldNotExposeStackTraceOrExceptionTextInValidationErrorBody() throws Exception {
        MvcResult result = mockMvc.perform(claim("invitee-1", claimBody("bad")))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertNoStackTraceOrInternals(result.getResponse().getContentAsString());
    }

    @Test
    void shouldNotExposeStackTraceOrExceptionTextInUnauthorizedBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rewards/referrals/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNoStackTraceOrInternals(result.getResponse().getContentAsString());
    }

    // ------------------------------------------------------------------ deposit flow

    @Test
    void shouldReturn200OnDepositWithPendingReferralAndNotExposeReferrerId() throws Exception {
        String referrerId = "referrer-secret-42";
        Referral pending = new Referral();
        pending.setInvitedUserId("invitee-1");
        pending.setReferrerUserId(referrerId);
        when(referralRepo.findByInvitedUserIdAndStatus("invitee-1", ReferralStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(referralRepo.markCredited(any(), any())).thenReturn(1);

        MvcResult result = mockMvc.perform(post("/api/rewards/deposit")
                        .header("X-User-Id", "invitee-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"loc-1\",\"barcode\":\"8718951234567\",\"materialType\":\"PET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsEarned").value(50))
                // 50 from the deposit plus the 50 first-deposit bonus for the invitee.
                .andExpect(jsonPath("$.totalPoints").value(100))
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains(referrerId));
        verify(referralRepo).markCredited(any(), any());
    }

    @Test
    void shouldReturn400InvalidRequestOnDepositWhenMaterialTypeIsMissing() throws Exception {
        mockMvc.perform(post("/api/rewards/deposit")
                        .header("X-User-Id", "invitee-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"loc-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void shouldReturn400InvalidRequestOnDepositWhenBarcodeIsMissingOrMalformed() throws Exception {
        for (String body : new String[]{
                "{\"materialType\":\"PET\"}",
                "{\"barcode\":\"12ab\",\"materialType\":\"PET\"}",
                "{\"barcode\":\"123456789012345\",\"materialType\":\"PET\"}"}) {
            mockMvc.perform(post("/api/rewards/deposit")
                            .header("X-User-Id", "u1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(scanClient);
    }

    @Test
    void shouldReturn400NotReturnableAndWriteNothingWhenScanServiceSaysNotEligible() throws Exception {
        when(scanClient.check("8718951234567"))
                .thenReturn(new com.ecoapp.reward.client.ScanDepositClient.DepositCheck(false, 0.0));
        mockMvc.perform(post("/api/rewards/deposit")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"8718951234567\",\"materialType\":\"PET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"NOT_RETURNABLE\",\"message\":\"This product cannot be returned for deposit\"}", true));
        verifyNoInteractions(depositRepo);
        verify(statsRepo, never()).save(any());
    }

    @Test
    void shouldReturn503WhenScanServiceUnavailable() throws Exception {
        when(scanClient.check(any())).thenThrow(com.ecoapp.reward.exception.DepositRefusedException.unavailable());
        mockMvc.perform(post("/api/rewards/deposit")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"8718951234567\",\"materialType\":\"PET\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"error\":\"DEPOSIT_UNAVAILABLE\",\"message\":\"Deposit could not be verified\"}", true));
        verifyNoInteractions(depositRepo);
    }

    @Test
    void shouldReturn401OnDepositWhenUserIdHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/rewards/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialType\":\"PET\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ helpers

    private void stubOwnCode(String userId, String code) {
        ReferralCode row = new ReferralCode();
        row.setUserId(userId);
        row.setCode(code);
        when(codeRepo.findByUserId(userId)).thenReturn(Optional.of(row));
    }

    private void stubCode(String code, String referrerId) {
        ReferralCode row = new ReferralCode();
        row.setUserId(referrerId);
        row.setCode(code);
        when(codeRepo.findByCode(code)).thenReturn(Optional.of(row));
    }

    private org.springframework.test.web.servlet.RequestBuilder claim(String userId, String body) {
        return post("/api/rewards/referrals/claim")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String claimBody(String code) {
        return "{\"code\":\"" + code + "\"}";
    }

    private org.springframework.test.web.servlet.RequestBuilder claimAfterUnknownCode() {
        when(codeRepo.findByCode("ZZZZZZZZ")).thenReturn(Optional.empty());
        return claim("invitee-1", claimBody("ZZZZZZZZ"));
    }

    private org.springframework.test.web.servlet.RequestBuilder claimOwnCode() {
        stubCode(VALID_CODE, "user-1");
        return claim("user-1", claimBody(VALID_CODE));
    }

    private org.springframework.test.web.servlet.RequestBuilder claimRepeat() {
        stubCode(VALID_CODE, "referrer-1");
        when(referralRepo.insertPendingIfAbsent(eq("invitee-1"), eq("referrer-1"), any())).thenReturn(0);
        return claim("invitee-1", claimBody(VALID_CODE));
    }

    private org.springframework.test.web.servlet.RequestBuilder claimAfterDeposit() {
        stubCode(VALID_CODE, "referrer-1");
        when(depositRepo.countByUserId("invitee-1")).thenReturn(3L);
        return claim("invitee-1", claimBody(VALID_CODE));
    }

    private void assertNoStackTraceOrInternals(String body) {
        assertFalse(body.contains("Exception"), "error body must not name an exception: " + body);
        assertFalse(body.contains("\tat "), "error body must not contain a stack trace");
        assertFalse(body.contains("com.ecoapp"), "error body must not reveal internal packages");
        assertFalse(body.toLowerCase().contains("sql"), "error body must not reveal persistence details");
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new java.util.ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) names.add(it.next());
        return names;
    }
}
