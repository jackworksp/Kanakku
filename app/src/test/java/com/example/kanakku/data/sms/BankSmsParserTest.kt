package com.example.kanakku.data.sms

import com.example.kanakku.data.model.SmsMessage
import com.example.kanakku.data.model.TransactionType
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BankSmsParser with comprehensive coverage of UPI transaction parsing.
 *
 * Tests cover:
 * - UPI transaction detection (Google Pay, PhonePe, Paytm, bank UPI apps)
 * - VPA (Virtual Payment Address) extraction
 * - UPI merchant/payee extraction
 * - UPI reference number extraction
 * - P2P and P2M UPI transaction parsing
 * - Credit and debit UPI transactions
 * - Generic bank SMS parsing
 * - Deduplication logic
 */
class BankSmsParserTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setup() {
        parser = BankSmsParser()
    }

    // ==================== Helper Functions ====================

    /**
     * Create a test SMS message with common defaults
     */
    private fun createSms(
        id: Long = 1L,
        address: String = "VM-HDFCBK",
        body: String,
        date: Long = System.currentTimeMillis(),
        isRead: Boolean = true
    ): SmsMessage {
        return SmsMessage(
            id = id,
            address = address,
            body = body,
            date = date,
            isRead = isRead
        )
    }

    // ==================== Test Fixtures: Google Pay SMS ====================

    private val googlePayDebitSms = createSms(
        id = 1L,
        address = "GOOGLEPAY",
        body = "You paid Rs.450 to Swiggy via Google Pay. UPI Ref No 123456789012. Download the app: http://g.co/payindia"
    )

    private val googlePayDebitWithVpaSms = createSms(
        id = 2L,
        address = "GPAY",
        body = "You sent Rs.250 to merchant@paytm using Google Pay. Google Ref ID 987654321098"
    )

    private val googlePayCreditSms = createSms(
        id = 3L,
        address = "G-PAY",
        body = "You received Rs.1500 from john.doe@oksbi via Google Pay. UPI Ref 456789012345"
    )

    private val googlePayP2PSms = createSms(
        id = 4L,
        address = "GPAY",
        body = "You paid Rs.100.00 to friend@paytm via Google Pay for Lunch. Ref: GP123456789"
    )

    private val googlePayDebitWithBalanceSms = createSms(
        id = 5L,
        address = "VM-GPAY",
        body = "Rs.750.50 debited from A/c XX8888 via Google Pay to petrol.pump@paytm on 02-Jan-26. Google Ref ID GR111222333. Avl Bal Rs.12450.75"
    )

    private val googlePayCreditFromMerchantSms = createSms(
        id = 6L,
        address = "AD-GPAY",
        body = "You received a refund of Rs.299.99 from amazon.pay@okicici via Google Pay. UPI Ref 789012345678"
    )

    private val googlePayTransferredSms = createSms(
        id = 7L,
        address = "GOOGLEPAY",
        body = "You transferred Rs.5000.00 to mom@oksbi using Google Pay for Monthly allowance. Ref: GP555666777"
    )

    private val googlePayWithAccountAndDateSms = createSms(
        id = 8L,
        address = "G-PAY",
        body = "Rs.1,250 debited from your A/c XX9999 on 02-Jan-26 via UPI to swiggy@paytm. Google Ref ID GP999888777. Balance Rs.8750"
    )

    private val googlePayLargeAmountSms = createSms(
        id = 9L,
        address = "GPAY",
        body = "You paid Rs.25,000.00 to furniture.store@axisbank via Google Pay. UPI Ref No 543210987654"
    )

    private val googlePayMinimalSms = createSms(
        id = 10L,
        address = "GPAY",
        body = "Paid Rs.50 to Tea Stall via GPay. Ref GP123"
    )

    private val googlePayWithCommaAmountSms = createSms(
        id = 11L,
        address = "GOOGLEPAY",
        body = "Rs.2,499.00 paid to flipkart@axisbank using Google Pay. Google Ref ID GP789456123"
    )

    private val googlePayCreditWithFullDetailsSms = createSms(
        id = 12L,
        address = "GPAY",
        body = "A/c XX7777 credited Rs.3,500.50 on 02-Jan-26. UPI received from employer@hdfcbank via Google Pay. Google Ref ID GP147258369. Avl Bal Rs.35500.50"
    )

    // ==================== Test Fixtures: PhonePe SMS ====================

    private val phonePeDebitSms = createSms(
        id = 5L,
        address = "PHONEPE",
        body = "Rs.599 debited from A/c XX1234 on 01-Jan-26. UPI paid to amazon.pay@icici. PhonePe Txn ID PP987654321. Avl Bal Rs.5000.50"
    )

    private val phonePeCreditSms = createSms(
        id = 6L,
        address = "PHONPE",
        body = "Rs.2000 credited to A/c XX5678 on 01-Jan-26. UPI received from employer@axisbank. PhonePe Txn ID PP123456789"
    )

    private val phonePeP2MSms = createSms(
        id = 7L,
        address = "AD-PHONEPE",
        body = "UPI payment of Rs.899 to zomato@paytm successful. PhonePe Txn ID PP555666777. Available Balance: Rs.3200"
    )

    private val phonePeRefundSms = createSms(
        id = 8L,
        address = "PHONEPE",
        body = "UPI refund of Rs.299 from flipkart@axisbank credited to your account. PhonePe Txn ID PP999888777"
    )

    private val phonePeP2PSms = createSms(
        id = 13L,
        address = "VM-PHONEPE",
        body = "You sent Rs.1,500.00 to brother@ybl via PhonePe UPI. PhonePe Txn ID PP246813579. Available Balance: Rs.8500.00"
    )

    private val phonePeLargeAmountSms = createSms(
        id = 14L,
        address = "AD-PHONEPE",
        body = "Rs.45,000 debited from A/c XX3456 on 02-Jan-26. UPI paid to furniture.store@okaxis. PhonePe Txn ID PP111222333. Avl Bal Rs.55000"
    )

    private val phonePeMinimalSms = createSms(
        id = 15L,
        address = "PHONPE",
        body = "Paid Rs.25 to Tea Shop via PhonePe. Ref PP789"
    )

    private val phonePeWithDateAndBalanceSms = createSms(
        id = 16L,
        address = "PHONEPE",
        body = "A/c XX6789 debited Rs.899.50 on 02-Jan-26. UPI payment to swiggy@paytm successful. PhonePe Txn ID PP369258147. Balance Rs.7200.50"
    )

    private val phonePeCreditWithDecimalSms = createSms(
        id = 17L,
        address = "VM-PHONPE",
        body = "Rs.2,750.75 UPI credited from client@hdfcbank to A/c XX4321 on 02-Jan-26. PhonePe Txn ID PP987123654. Avl Bal Rs.12750.75"
    )

    private val phonePeTransferredSms = createSms(
        id = 18L,
        address = "PHONEPE",
        body = "You transferred Rs.3,200 to mom@oksbi using PhonePe for Monthly support. PhonePe Txn ID PP555444333"
    )

    private val phonePeWithCommaSms = createSms(
        id = 19L,
        address = "AD-PHONPE",
        body = "Rs.1,999.00 paid to amazon.pay@icici using PhonePe UPI. PhonePe Txn ID PP654987321. Bal: Rs.5001"
    )

    // ==================== Test Fixtures: Paytm SMS ====================

    private val paytmDebitSms = createSms(
        id = 9L,
        address = "PAYTM",
        body = "Rs.350 debited from A/c XX9876 via UPI to bookmyshow@paytm. Paytm Txn ID PTM123456789. Bal: Rs.4500"
    )

    private val paytmCreditSms = createSms(
        id = 10L,
        address = "PYTMPA",
        body = "Rs.750 UPI credited from client@okicici to A/c XX9876. Paytm Txn ID PTM987654321. Avl Bal: Rs.5250"
    )

    private val paytmP2PSms = createSms(
        id = 11L,
        address = "VM-PAYTM",
        body = "You sent Rs.500 to sister.name@ybl via Paytm UPI. Txn ID: PTM111222333. Balance Rs.4750"
    )

    private val paytmMerchantSms = createSms(
        id = 12L,
        address = "PAYTM",
        body = "UPI txn of Rs.1299 to Reliance Digital completed. Ref No PTM444555666. A/c XX9876"
    )

    private val paytmLargeAmountSms = createSms(
        id = 20L,
        address = "AD-PAYTM",
        body = "Rs.35,000 debited from A/c XX9876 on 02-Jan-26. UPI paid to furniture.mart@okaxis. Paytm Txn ID PTM777888999. Avl Bal Rs.65000"
    )

    private val paytmMinimalSms = createSms(
        id = 21L,
        address = "PAYTM",
        body = "Paid Rs.30 to Chai Stall via Paytm. Ref PTM321"
    )

    private val paytmWithDateAndBalanceSms = createSms(
        id = 22L,
        address = "VM-PAYTM",
        body = "A/c XX9876 debited Rs.750.25 on 02-Jan-26. UPI payment to bigbasket@paytm successful. Paytm Txn ID PTM147852369. Balance Rs.9250.75"
    )

    private val paytmCreditWithDecimalSms = createSms(
        id = 23L,
        address = "PYTMPA",
        body = "Rs.1,850.50 UPI credited from freelance@hdfcbank to A/c XX9876 on 02-Jan-26. Paytm Txn ID PTM654321987. Avl Bal Rs.11101.25"
    )

    private val paytmTransferredSms = createSms(
        id = 24L,
        address = "PAYTM",
        body = "You transferred Rs.2,500 to dad@oksbi using Paytm for Monthly support. Paytm Txn ID PTM999111222"
    )

    private val paytmWithCommaSms = createSms(
        id = 25L,
        address = "AD-PAYTM",
        body = "Rs.1,599.00 paid to myntra@paytm using Paytm UPI. Paytm Txn ID PTM888777666. Bal: Rs.7401"
    )

    private val paytmRefundSms = createSms(
        id = 26L,
        address = "PYTMPA",
        body = "UPI refund of Rs.499 from amazon.pay@icici credited to your account. Paytm Txn ID PTM555444333"
    )

    private val paytmP2MSms = createSms(
        id = 27L,
        address = "VM-PAYTM",
        body = "UPI payment of Rs.1199 to dominos@paytm successful. Paytm Txn ID PTM222333444. Available Balance: Rs.5801"
    )

    private val paytmWithFullDetailsSms = createSms(
        id = 28L,
        address = "PAYTM",
        body = "A/c XX9876 credited Rs.4,500.75 on 02-Jan-26. UPI received from project.pay@axisbank via Paytm. Paytm Txn ID PTM963852741. Avl Bal Rs.45500.75"
    )

    private val paytmAlternativeSenderSms = createSms(
        id = 29L,
        address = "PYTM",
        body = "Rs.899 debited via UPI to zomato@paytm. Txn ID: PTM123789456. Balance Rs.3101"
    )

    // ==================== Test Fixtures: Bank UPI SMS ====================

    private val hdfcUpiDebitSms = createSms(
        id = 13L,
        address = "HDFCUPI",
        body = "Rs.800 debited from A/c XX4321 on 01-Jan via UPI to uber@paytm. UPI Ref 202401010012345. Avl Bal Rs.12000"
    )

    private val sbiUpiDebitSms = createSms(
        id = 14L,
        address = "SBIYONO",
        body = "Your A/c XX8765 debited with Rs.1200 on 01-Jan-26. UPI payment to rapido@axisbank. UTR: 402401010054321"
    )

    private val iciciUpiCreditSms = createSms(
        id = 15L,
        address = "ICICIM",
        body = "A/c XX6543 credited Rs.3500 on 01-Jan-26. UPI received from company@hdfcbank. UPI Txn ID ICI123456789012"
    )

    private val axisUpiDebitSms = createSms(
        id = 16L,
        address = "AXISBK",
        body = "Rs.650 debited via UPI from A/c XX3210 to olacabs@okaxis. Axis UPI Ref: AX987654321098. Balance: Rs.8500"
    )

    private val kotakUpiSms = createSms(
        id = 17L,
        address = "KOTAKUPI",
        body = "UPI transfer of Rs.2500 from A/c XX7890 to landlord@ybl completed. Ref No KTK123456789. Bal Rs.15000"
    )

    private val idbiUpiSms = createSms(
        id = 18L,
        address = "IDFCPAY",
        body = "Rs.999 paid to netflix@icici via UPI from A/c XX4567. Txn ID IDFC987654321. Avl Bal Rs.6500"
    )

    // ==================== Test Fixtures: Additional Bank UPI SMS (Comprehensive) ====================

    // HDFC UPI - Additional test cases
    private val hdfcUpiCreditSms = createSms(
        id = 30L,
        address = "HDFCPAY",
        body = "A/c XX1234 credited Rs.5,500.00 on 02-Jan-26. UPI received from salary@company via HDFC UPI. UPI Ref 202601020098765. Avl Bal Rs.45500.00"
    )

    private val hdfcUpiP2PSms = createSms(
        id = 31L,
        address = "VM-HDFCUPI",
        body = "You sent Rs.1,200.50 to friend@paytm via HDFC UPI. UPI Ref No 202601020012346. Balance Rs.11299.50"
    )

    private val hdfcUpiMinimalSms = createSms(
        id = 32L,
        address = "HDFCPAY",
        body = "Paid Rs.45 to Tea Shop via UPI. Ref 202601020000111"
    )

    private val hdfcUpiLargeAmountSms = createSms(
        id = 33L,
        address = "HDFCUPI",
        body = "Rs.75,000 debited from A/c XX1234 on 02-Jan-26. UPI payment to property@axisbank successful. UPI Ref 202601020099999. Avl Bal Rs.125000"
    )

    private val hdfcUpiWithCommaSms = createSms(
        id = 34L,
        address = "VM-HDFCPAY",
        body = "Rs.3,750.75 debited via UPI to amazon.pay@icici from A/c XX1234. UPI Ref No 202601020054321. Balance Rs.21249.25"
    )

    // SBI UPI - Additional test cases
    private val sbiUpiCreditSms = createSms(
        id = 35L,
        address = "SBIPAY",
        body = "A/c XX5678 credited Rs.8,250.00 on 02-Jan-26. UPI received from freelance@hdfcbank. UTR: 402601020087654. Avl Bal Rs.28250.00"
    )

    private val sbiUpiP2PSms = createSms(
        id = 36L,
        address = "YONOSBI",
        body = "You transferred Rs.2,100.00 to sister@ybl using SBI UPI for Gift. UTR: 402601020011111. Balance Rs.18900"
    )

    private val sbiUpiMinimalSms = createSms(
        id = 37L,
        address = "SBIUPI",
        body = "Paid Rs.65 to Snacks via UPI. UTR: 402601020000222"
    )

    private val sbiUpiLargeAmountSms = createSms(
        id = 38L,
        address = "SBIYONO",
        body = "Rs.95,500 debited from A/c XX5678 on 02-Jan-26. UPI paid to jewellery.store@okaxis. UTR: 402601020099999. Avl Bal Rs.204500"
    )

    private val sbiUpiRefundSms = createSms(
        id = 39L,
        address = "VM-SBIPAY",
        body = "UPI refund of Rs.1,299.50 from flipkart@axisbank credited to A/c XX5678. UTR: 402601020076543. Bal: Rs.9799.50"
    )

    private val sbiUpiWithDecimalSms = createSms(
        id = 40L,
        address = "SBIUPI",
        body = "Rs.4,899.99 debited via UPI to swiggy@paytm from A/c XX5678 on 02-Jan-26. UTR: 402601020055555. Balance Rs.14100.01"
    )

    // ICICI UPI - Additional test cases
    private val iciciUpiDebitSms = createSms(
        id = 41L,
        address = "ICICIM",
        body = "Rs.2,750 debited from A/c XX3456 on 02-Jan-26. UPI paid to zomato@paytm. UPI Txn ID ICI202601020012345. Avl Bal Rs.17250"
    )

    private val iciciUpiP2PSms = createSms(
        id = 42L,
        address = "ICICIPAY",
        body = "You sent Rs.5,000.00 to brother@ybl via ICICI UPI for Emergency. UPI Txn ID ICI202601020098765. Balance Rs.25000.00"
    )

    private val iciciUpiMinimalSms = createSms(
        id = 43L,
        address = "VM-ICICIM",
        body = "Paid Rs.55 to Coffee Shop via UPI. Ref ICI202601020000333"
    )

    private val iciciUpiLargeAmountSms = createSms(
        id = 44L,
        address = "ICICIBK",
        body = "Rs.1,25,000 debited from A/c XX3456 on 02-Jan-26. UPI payment to car.dealer@hdfcbank successful. UPI Txn ID ICI202601020099999. Avl Bal Rs.375000"
    )

    private val iciciUpiRefundSms = createSms(
        id = 45L,
        address = "ICICIPAY",
        body = "UPI refund of Rs.2,499.00 from amazon.pay@icici credited to your A/c XX3456. UPI Txn ID ICI202601020087654"
    )

    private val iciciUpiWithDecimalSms = createSms(
        id = 46L,
        address = "AD-ICICIM",
        body = "Rs.6,750.25 UPI debited to bigbasket@paytm from A/c XX3456 on 02-Jan-26. UPI Txn ID ICI202601020066666. Bal Rs.23249.75"
    )

    // Axis UPI - Additional test cases
    private val axisUpiCreditSms = createSms(
        id = 47L,
        address = "AXISPAY",
        body = "A/c XX9876 credited Rs.12,500.00 on 02-Jan-26. UPI received from project@hdfcbank. Axis UPI Ref: AX202601020087654. Avl Bal Rs.52500.00"
    )

    private val axisUpiP2PSms = createSms(
        id = 48L,
        address = "VM-AXISBK",
        body = "You transferred Rs.3,500.00 to mom@oksbi using Axis UPI for Monthly support. Axis UPI Ref: AX202601020012345. Balance Rs.16500.00"
    )

    private val axisUpiMinimalSms = createSms(
        id = 49L,
        address = "AXISBNK",
        body = "Paid Rs.70 to Grocery via UPI. Ref AX202601020000444"
    )

    private val axisUpiLargeAmountSms = createSms(
        id = 50L,
        address = "AXISBK",
        body = "Rs.2,15,000 debited from A/c XX9876 on 02-Jan-26. UPI paid to furniture.mart@okaxis. Axis UPI Ref: AX202601020099999. Avl Bal Rs.285000"
    )

    private val axisUpiRefundSms = createSms(
        id = 51L,
        address = "AXISPAY",
        body = "UPI refund of Rs.3,799.00 from myntra@paytm credited to A/c XX9876. Axis UPI Ref: AX202601020076543. Bal: Rs.23799"
    )

    private val axisUpiWithDecimalSms = createSms(
        id = 52L,
        address = "AD-AXISBNK",
        body = "Rs.8,999.50 debited via UPI to petrol.pump@paytm from A/c XX9876 on 02-Jan-26. Axis UPI Ref: AX202601020055555. Balance Rs.41000.50"
    )

    // ==================== Test Fixtures: BHIM and NPCI UPI ====================

    private val bhimUpiDebitSms = createSms(
        id = 19L,
        address = "BHIM",
        body = "Rs.150 debited from A/c XX1111 via BHIM UPI to teashop@paytm. UPI Ref 202401010011111"
    )

    private val npciUpiSms = createSms(
        id = 20L,
        address = "NPCIUPI",
        body = "UPI payment of Rs.5000 to property.dealer@okicici successful. UPI Transaction ID NPCI12345678901234"
    )

    // ==================== Test Fixtures: Edge Cases ====================

    private val upiSmsWithoutVpaSms = createSms(
        id = 21L,
        address = "GPAY",
        body = "You paid Rs.75 to Local Store via Google Pay. Google Ref ID GP111222333"
    )

    private val upiSmsMultipleAmountsSms = createSms(
        id = 22L,
        address = "PHONEPE",
        body = "Rs.500 debited. Cashback Rs.50 credited. Net payment Rs.450 to BigBasket. PhonePe Txn ID PP123"
    )

    private val upiSmsWithLocationSms = createSms(
        id = 23L,
        address = "PAYTM",
        body = "Rs.200 UPI paid to Store Name, Mumbai on 01-Jan-26. Paytm Txn ID PTM789"
    )

    private val upiOtpSms = createSms(
        id = 24L,
        address = "GPAY",
        body = "Your Google Pay OTP is 123456. Do not share this code with anyone. Valid for 10 minutes."
    )

    private val genericBankDebitSms = createSms(
        id = 25L,
        address = "VM-HDFCBK",
        body = "Rs.5000 debited from A/c XX1234 on 01-Jan-26 at AMAZON. Ref 123456789012. Avl Bal Rs.25000"
    )

    private val genericBankCreditSms = createSms(
        id = 26L,
        address = "AD-SBIBNK",
        body = "Rs.15000 credited to A/c XX5678 on 01-Jan-26. Salary deposit. Ref NEFT123456789. Bal Rs.40000"
    )

    private val atmWithdrawalSms = createSms(
        id = 27L,
        address = "VM-ICICIBANK",
        body = "Rs.2000 withdrawn from A/c XX9999 at HDFC ATM CHURCHGATE BR on 01-Jan-26. Avl Bal Rs.23000"
    )

    // ==================== UPI Transaction Detection Tests ====================

    @Test
    fun isUpiTransactionSms_detectsGooglePay() {
        // Given
        val sms = googlePayDebitSms

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertTrue(result)
    }

    @Test
    fun isUpiTransactionSms_detectsPhonePe() {
        // Given
        val sms = phonePeDebitSms

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertTrue(result)
    }

    @Test
    fun isUpiTransactionSms_detectsPaytm() {
        // Given
        val sms = paytmDebitSms

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertTrue(result)
    }

    @Test
    fun isUpiTransactionSms_detectsBankUpi() {
        // Given
        val sms = hdfcUpiDebitSms

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertTrue(result)
    }

    @Test
    fun isUpiTransactionSms_detectsUpiKeywordInBody() {
        // Given - Bank sender with UPI keyword in body
        val sms = createSms(
            address = "VM-HDFCBK",
            body = "Rs.100 debited via UPI to merchant@paytm. Ref 123456"
        )

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertTrue(result)
    }

    @Test
    fun isUpiTransactionSms_filtersOutOtp() {
        // Given
        val sms = upiOtpSms

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertFalse(result)
    }

    @Test
    fun isUpiTransactionSms_rejectsNonUpiSms() {
        // Given
        val sms = genericBankDebitSms

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertFalse(result)
    }

    @Test
    fun isUpiTransactionSms_requiresAmount() {
        // Given
        val sms = createSms(
            address = "GPAY",
            body = "Payment to merchant@paytm successful. Ref GP123"
        )

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertFalse(result)
    }

    @Test
    fun isUpiTransactionSms_requiresDebitOrCreditKeyword() {
        // Given
        val sms = createSms(
            address = "GPAY",
            body = "Transaction of Rs.100 to merchant@paytm. Ref GP123"
        )

        // When
        val result = parser.isUpiTransactionSms(sms)

        // Then
        assertFalse(result)
    }

    // ==================== VPA Extraction Tests ====================

    @Test
    fun extractVpa_extractsFromPaytmHandle() {
        // Given
        val body = "You paid Rs.450 to merchant@paytm via Google Pay"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("merchant@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsFromOkAxisHandle() {
        // Given
        val body = "Rs.599 paid to amazon.pay@okaxis. PhonePe Txn ID PP987"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("amazon.pay@okaxis", vpa)
    }

    @Test
    fun extractVpa_extractsFromYblHandle() {
        // Given
        val body = "Rs.500 sent to john.doe@ybl via Paytm UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("john.doe@ybl", vpa)
    }

    @Test
    fun extractVpa_extractsFromIciciHandle() {
        // Given
        val body = "UPI received from company@hdfcbank. Ref 123456"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("company@hdfcbank", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_to() {
        // Given
        val body = "Payment to swiggy@axisbank successful"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("swiggy@axisbank", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_from() {
        // Given
        val body = "Money received from friend@paytm"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("friend@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_paidTo() {
        // Given
        val body = "Rs.100 paid to zomato@paytm"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("zomato@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_receivedFrom() {
        // Given
        val body = "Rs.500 received from employer@okicici"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("employer@okicici", vpa)
    }

    @Test
    fun extractVpa_returnsNullWhenNotFound() {
        // Given
        val body = "Rs.100 debited from A/c XX1234. No UPI ID present"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertNull(vpa)
    }

    @Test
    fun extractVpa_handlesMultipleVpas_prefersContextual() {
        // Given
        val body = "Sent from user1@paytm to user2@okaxis via UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        // Should prefer contextual match (to user2@okaxis)
        assertEquals("user2@okaxis", vpa)
    }

    // Additional VPA format tests for various UPI ID formats

    @Test
    fun extractVpa_extractsWithUnderscore() {
        // Given
        val body = "Rs.500 paid to user_name@paytm via UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("user_name@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsWithMultipleDots() {
        // Given
        val body = "Payment to first.middle.last@okaxis successful"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("first.middle.last@okaxis", vpa)
    }

    @Test
    fun extractVpa_extractsWithHyphen() {
        // Given
        val body = "You sent Rs.250 to user-name@ybl using PhonePe"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("user-name@ybl", vpa)
    }

    @Test
    fun extractVpa_extractsWithNumbers() {
        // Given
        val body = "Rs.300 debited to user123@paytm. Ref 456789"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("user123@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsWithDotsAndNumbers() {
        // Given
        val body = "UPI payment to user.123@okicici completed"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("user.123@okicici", vpa)
    }

    @Test
    fun extractVpa_extractsWithMixedFormat() {
        // Given
        val body = "Rs.750 sent to user_123.name@hdfcbank via UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("user_123.name@hdfcbank", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_VPA() {
        // Given
        val body = "UPI transfer successful. VPA: merchant@axisbank Ref 123456"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("merchant@axisbank", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_VPAColon() {
        // Given
        val body = "Transaction to VPA:swiggy@okhdfc completed"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("swiggy@okhdfc", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_UpiId() {
        // Given
        val body = "Payment sent. UPI ID: zomato@oksbi Txn ID 789012"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("zomato@oksbi", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_UpiIdColon() {
        // Given
        val body = "UPI ID:amazon.pay@icici. Amount Rs.500"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("amazon.pay@icici", vpa)
    }

    @Test
    fun extractVpa_extractsWithContext_sentTo() {
        // Given
        val body = "You sent Rs.1000 sent to landlord@sbi via PhonePe"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("landlord@sbi", vpa)
    }

    @Test
    fun extractVpa_extractsFromSbiHandle() {
        // Given
        val body = "Rs.450 debited to friend@sbi. UPI Ref 123456789"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("friend@sbi", vpa)
    }

    @Test
    fun extractVpa_extractsFromSbiBankHandle() {
        // Given
        val body = "Payment to employer@sbibank successful"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("employer@sbibank", vpa)
    }

    @Test
    fun extractVpa_extractsFromAxisHandle() {
        // Given
        val body = "Rs.899 paid to store@axis via Google Pay"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("store@axis", vpa)
    }

    @Test
    fun extractVpa_extractsFromKotakHandle() {
        // Given
        val body = "UPI received from client@kotak. Amount Rs.5000"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("client@kotak", vpa)
    }

    @Test
    fun extractVpa_extractsFromKotakBankHandle() {
        // Given
        val body = "Rs.1200 sent to vendor@kotakbank via UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("vendor@kotakbank", vpa)
    }

    @Test
    fun extractVpa_extractsFromFederalHandle() {
        // Given
        val body = "Payment to merchant@federal completed. Ref 567890"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("merchant@federal", vpa)
    }

    @Test
    fun extractVpa_extractsFromIdfcHandle() {
        // Given
        val body = "Rs.2500 debited to service@idfc on 02-Jan"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("service@idfc", vpa)
    }

    @Test
    fun extractVpa_extractsFromIdfcBankHandle() {
        // Given
        val body = "You paid Rs.350 to shop@idfcbank via PhonePe"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("shop@idfcbank", vpa)
    }

    @Test
    fun extractVpa_extractsFromYesBankHandle() {
        // Given
        val body = "UPI payment to contractor@yesbank. Txn ID 123456"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("contractor@yesbank", vpa)
    }

    @Test
    fun extractVpa_extractsFromRblHandle() {
        // Given
        val body = "Rs.750 sent to partner@rbl via Google Pay"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("partner@rbl", vpa)
    }

    @Test
    fun extractVpa_extractsFromIblHandle() {
        // Given
        val body = "Payment received from customer@ibl. Ref 987654"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("customer@ibl", vpa)
    }

    @Test
    fun extractVpa_extractsFromAxlHandle() {
        // Given
        val body = "Rs.1500 debited to seller@axl on 01-Jan-26"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("seller@axl", vpa)
    }

    @Test
    fun extractVpa_extractsFromAplHandle() {
        // Given
        val body = "UPI transfer to provider@apl completed successfully"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("provider@apl", vpa)
    }

    @Test
    fun extractVpa_extractsFromIndianBankHandle() {
        // Given
        val body = "You paid Rs.600 to outlet@indianbank via UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("outlet@indianbank", vpa)
    }

    @Test
    fun extractVpa_extractsFromPnbHandle() {
        // Given
        val body = "Rs.950 sent to business@pnb. PhonePe Txn ID PP123"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("business@pnb", vpa)
    }

    @Test
    fun extractVpa_extractsFromBobHandle() {
        // Given
        val body = "Payment to supplier@bob successful. Ref 456789"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("supplier@bob", vpa)
    }

    @Test
    fun extractVpa_extractsFromUnionBankHandle() {
        // Given
        val body = "Rs.1800 debited to dealer@unionbank via Google Pay"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("dealer@unionbank", vpa)
    }

    @Test
    fun extractVpa_extractsFromCanaraHandle() {
        // Given
        val body = "UPI received from agent@canara. Amount Rs.3000"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("agent@canara", vpa)
    }

    @Test
    fun extractVpa_extractsFromCanaraBankHandle() {
        // Given
        val body = "You sent Rs.2200 to trader@canarabank via Paytm"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("trader@canarabank", vpa)
    }

    @Test
    fun extractVpa_extractsFromBarodaPayHandle() {
        // Given
        val body = "Rs.1100 paid to retailer@barodapay. UPI Ref 789123"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("retailer@barodapay", vpa)
    }

    @Test
    fun extractVpa_extractsFromFreechargeHandle() {
        // Given
        val body = "Payment to wallet@freecharge completed successfully"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("wallet@freecharge", vpa)
    }

    @Test
    fun extractVpa_extractsFromMobikwikHandle() {
        // Given
        val body = "Rs.450 sent to recharge@mobikwik via UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("recharge@mobikwik", vpa)
    }

    @Test
    fun extractVpa_extractsFromAirtelHandle() {
        // Given
        val body = "UPI payment to payment@airtel. Txn ID 112233"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("payment@airtel", vpa)
    }

    @Test
    fun extractVpa_extractsFromUpiHandle() {
        // Given
        val body = "You paid Rs.550 to generic@upi via PhonePe"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("generic@upi", vpa)
    }

    @Test
    fun extractVpa_extractsVpaAtStartOfMessage() {
        // Given
        val body = "merchant@paytm received Rs.750 via Google Pay"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("merchant@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsVpaAtEndOfMessage() {
        // Given
        val body = "Rs.899 UPI payment completed to shop@okaxis"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("shop@okaxis", vpa)
    }

    @Test
    fun extractVpa_extractsWithMultipleHyphens() {
        // Given
        val body = "Payment to user-with-many-hyphens@paytm successful"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("user-with-many-hyphens@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsWithMultipleUnderscores() {
        // Given
        val body = "Rs.650 sent to user_with_many_underscores@ybl"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("user_with_many_underscores@ybl", vpa)
    }

    @Test
    fun extractVpa_extractsComplexMixedFormat() {
        // Given
        val body = "UPI to first.middle_last-name123@hdfcbank. Ref 555666"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("first.middle_last-name123@hdfcbank", vpa)
    }

    @Test
    fun extractVpa_extractsCaseInsensitive() {
        // Given
        val body = "You paid Rs.400 to MerChant@PAYTM via Google Pay"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        // VPA should be returned in lowercase
        assertEquals("merchant@paytm", vpa)
    }

    @Test
    fun extractVpa_extractsFromIciciBankHandle() {
        // Given
        val body = "Rs.1250 debited to store@icicibank via UPI"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("store@icicibank", vpa)
    }

    @Test
    fun extractVpa_extractsFromDifferentContextKeywords() {
        // Given
        val body = "UPI transfer of Rs.2000 transferred to recipient@okaxis"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        // Note: "transferred to" is not a direct context keyword but "to" is
        assertEquals("recipient@okaxis", vpa)
    }

    @Test
    fun extractVpa_extractsWithMinimalUsernameLength() {
        // Given - minimum 3 characters for username (based on regex {2,} which is 2+ after first char = 3 total)
        val body = "Payment to abc@paytm successful"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("abc@paytm", vpa)
    }

    @Test
    fun extractVpa_handlesVpaWithSpecialMerchantNames() {
        // Given
        val body = "Rs.1999 paid to amazon.pay.store@okicici via PhonePe"

        // When
        val vpa = parser.extractVpa(body)

        // Then
        assertEquals("amazon.pay.store@okicici", vpa)
    }

    // ==================== Merchant Extraction Tests ====================

    @Test
    fun extractUpiMerchant_extractsFromPaidTo() {
        // Given
        val body = "Rs.450 paid to Swiggy via Google Pay. Ref 123"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("Swiggy", merchant)
    }

    @Test
    fun extractUpiMerchant_extractsFromTo() {
        // Given
        val body = "Rs.599 to Amazon Pay. PhonePe Txn ID PP987"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("Amazon Pay", merchant)
    }

    @Test
    fun extractUpiMerchant_extractsFromSentTo() {
        // Given
        val body = "You sent Rs.250 to Zomato using Google Pay"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("Zomato", merchant)
    }

    @Test
    fun extractUpiMerchant_extractsFromReceivedFrom() {
        // Given
        val body = "You received Rs.1500 from John Doe via Google Pay"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("John Doe", merchant)
    }

    @Test
    fun extractUpiMerchant_extractsFromTransferredTo() {
        // Given
        val body = "UPI transfer of Rs.2500 transferred to Landlord completed"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("Landlord", merchant)
    }

    @Test
    fun extractUpiMerchant_normalizesName() {
        // Given
        val body = "Rs.100 paid to   MERCHANT  NAME   on 01-Jan"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("Merchant Name", merchant)
    }

    @Test
    fun extractUpiMerchant_removesBusinessSuffixes() {
        // Given
        val body = "Rs.500 to ABC Company Pvt Ltd. Ref 123"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("Abc Company", merchant)
    }

    @Test
    fun extractUpiMerchant_handlesSpecialCharacters() {
        // Given
        val body = "Rs.200 paid to Store & Co. on 01-Jan"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertEquals("Store & Co", merchant)
    }

    @Test
    fun extractUpiMerchant_fallsBackToVpa() {
        // Given - No explicit merchant, but VPA present
        val body = "Rs.450 to merchant.name@paytm. Ref 123"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then - Should extract from VPA
        assertEquals("Merchant Name", merchant)
    }

    @Test
    fun extractUpiMerchant_returnsNullWhenNotFound() {
        // Given
        val body = "Rs.100 debited. Ref 123456"

        // When
        val merchant = parser.extractUpiMerchant(body)

        // Then
        assertNull(merchant)
    }

    // ==================== Merchant from VPA Tests ====================

    @Test
    fun extractMerchantFromVpa_extractsSimpleName() {
        // Given
        val vpa = "swiggy@axisbank"

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then
        assertEquals("Swiggy", merchant)
    }

    @Test
    fun extractMerchantFromVpa_extractsDottedName() {
        // Given
        val vpa = "amazon.pay@icici"

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then
        assertEquals("Amazon Pay", merchant)
    }

    @Test
    fun extractMerchantFromVpa_extractsUnderscoredName() {
        // Given
        val vpa = "uber_india@paytm"

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then
        assertEquals("Uber India", merchant)
    }

    @Test
    fun extractMerchantFromVpa_extractsHyphenatedName() {
        // Given
        val vpa = "book-my-show@okaxis"

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then
        assertEquals("Book My Show", merchant)
    }

    @Test
    fun extractMerchantFromVpa_removesNumbers() {
        // Given
        val vpa = "merchant123@paytm"

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then
        assertEquals("Merchant", merchant)
    }

    @Test
    fun extractMerchantFromVpa_keepsNumbersIfMeaningful() {
        // Given - "12" is too short after removing numbers
        val vpa = "ab12@paytm"

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then - Should keep numbers for meaningful result
        assertEquals("Ab12", merchant)
    }

    @Test
    fun extractMerchantFromVpa_returnsNullForShortUsername() {
        // Given
        val vpa = "ab@paytm"

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then
        assertNull(merchant)
    }

    @Test
    fun extractMerchantFromVpa_returnsNullForNullInput() {
        // Given
        val vpa: String? = null

        // When
        val merchant = parser.extractMerchantFromVpa(vpa)

        // Then
        assertNull(merchant)
    }

    // ==================== UPI Reference Extraction Tests ====================

    @Test
    fun extractUpiReference_extractsUpiRef() {
        // Given
        val body = "You paid Rs.450 to Swiggy. UPI Ref 123456789012"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("123456789012", ref)
    }

    @Test
    fun extractUpiReference_extractsUpiRefNo() {
        // Given
        val body = "Payment successful. UPI Ref No 987654321098"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("987654321098", ref)
    }

    @Test
    fun extractUpiReference_extractsGoogleRefId() {
        // Given
        val body = "Transaction complete. Google Ref ID 123456789"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("123456789", ref)
    }

    @Test
    fun extractUpiReference_extractsPhonePeTxnId() {
        // Given
        val body = "Payment done. PhonePe Txn ID PP987654321"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("PP987654321", ref)
    }

    @Test
    fun extractUpiReference_extractsPaytmTxnId() {
        // Given
        val body = "Sent via Paytm. Paytm Txn ID PTM123456789"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("PTM123456789", ref)
    }

    @Test
    fun extractUpiReference_extractsGenericTxnId() {
        // Given
        val body = "Payment successful. Txn ID ABC123456789"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("ABC123456789", ref)
    }

    @Test
    fun extractUpiReference_extractsUtr() {
        // Given
        val body = "Payment completed. UTR: 402401010054321"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("402401010054321", ref)
    }

    @Test
    fun extractUpiReference_prioritizesUpiSpecificFormat() {
        // Given - Both UPI-specific and generic ref present
        val body = "UPI Ref 123456789012. Ref: 999999999999"

        // When
        val ref = parser.extractUpiReference(body)

        // Then - Should prefer UPI-specific
        assertEquals("123456789012", ref)
    }

    @Test
    fun extractUpiReference_fallsBackToGenericRef() {
        // Given - Only generic ref present
        val body = "Payment done. Ref: 555666777888"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertEquals("555666777888", ref)
    }

    @Test
    fun extractUpiReference_returnsNullWhenNotFound() {
        // Given
        val body = "Payment completed successfully"

        // When
        val ref = parser.extractUpiReference(body)

        // Then
        assertNull(ref)
    }

    // ==================== Google Pay Parsing Tests ====================

    @Test
    fun parseUpiSms_parsesGooglePayDebit() {
        // Given
        val sms = googlePayDebitSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Swiggy", result.merchant)
        assertEquals("123456789012", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in this message
    }

    @Test
    fun parseUpiSms_parsesGooglePayWithVpa() {
        // Given
        val sms = googlePayDebitWithVpaSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("merchant@paytm", result.upiId)
        assertEquals("Merchant", result.merchant) // Extracted from VPA
        assertEquals("987654321098", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayCredit() {
        // Given
        val sms = googlePayCreditSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("john.doe@oksbi", result.upiId)
        assertEquals("John Doe", result.merchant) // Extracted from VPA
        assertEquals("456789012345", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayP2P() {
        // Given
        val sms = googlePayP2PSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("friend@paytm", result.upiId)
        assertEquals("Friend", result.merchant)
        assertEquals("GP123456789", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayDebitWithBalance() {
        // Given
        val sms = googlePayDebitWithBalanceSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(750.50, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("petrol.pump@paytm", result.upiId)
        assertEquals("Petrol Pump", result.merchant)
        assertEquals("8888", result.accountNumber)
        assertEquals("GR111222333", result.referenceNumber)
        assertEquals(12450.75, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayCreditFromMerchant() {
        // Given
        val sms = googlePayCreditFromMerchantSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(299.99, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("amazon.pay@okicici", result.upiId)
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("789012345678", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayTransferred() {
        // Given
        val sms = googlePayTransferredSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("mom@oksbi", result.upiId)
        assertEquals("Mom", result.merchant)
        assertEquals("GP555666777", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayWithAccountAndDate() {
        // Given
        val sms = googlePayWithAccountAndDateSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1250.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("swiggy@paytm", result.upiId)
        assertEquals("Swiggy", result.merchant)
        assertEquals("9999", result.accountNumber)
        assertEquals("GP999888777", result.referenceNumber)
        assertEquals(8750.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayLargeAmount() {
        // Given
        val sms = googlePayLargeAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(25000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("furniture.store@axisbank", result.upiId)
        assertEquals("Furniture Store", result.merchant)
        assertEquals("543210987654", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayMinimal() {
        // Given
        val sms = googlePayMinimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(50.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Tea Stall", result.merchant)
        assertEquals("GP123", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in minimal message
    }

    @Test
    fun parseUpiSms_parsesGooglePayWithCommaAmount() {
        // Given
        val sms = googlePayWithCommaAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2499.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("flipkart@axisbank", result.upiId)
        assertEquals("Flipkart", result.merchant)
        assertEquals("GP789456123", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesGooglePayCreditWithFullDetails() {
        // Given
        val sms = googlePayCreditWithFullDetailsSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(3500.50, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("employer@hdfcbank", result.upiId)
        assertEquals("Employer", result.merchant)
        assertEquals("7777", result.accountNumber)
        assertEquals("GP147258369", result.referenceNumber)
        assertEquals(35500.50, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== PhonePe Parsing Tests ====================

    @Test
    fun parseUpiSms_parsesPhonePeDebit() {
        // Given
        val sms = phonePeDebitSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(599.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("amazon.pay@icici", result.upiId)
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("1234", result.accountNumber)
        assertEquals("PP987654321", result.referenceNumber)
        assertEquals(5000.50, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeCredit() {
        // Given
        val sms = phonePeCreditSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("employer@axisbank", result.upiId)
        assertEquals("Employer", result.merchant)
        assertEquals("5678", result.accountNumber)
        assertEquals("PP123456789", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeP2M() {
        // Given
        val sms = phonePeP2MSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(899.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("zomato@paytm", result.upiId)
        assertEquals("Zomato", result.merchant)
        assertEquals("PP555666777", result.referenceNumber)
        assertEquals(3200.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeRefund() {
        // Given
        val sms = phonePeRefundSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(299.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("flipkart@axisbank", result.upiId)
        assertEquals("Flipkart", result.merchant)
        assertEquals("PP999888777", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeP2P() {
        // Given
        val sms = phonePeP2PSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("brother@ybl", result.upiId)
        assertEquals("Brother", result.merchant)
        assertEquals("PP246813579", result.referenceNumber)
        assertEquals(8500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeLargeAmount() {
        // Given
        val sms = phonePeLargeAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(45000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("furniture.store@okaxis", result.upiId)
        assertEquals("Furniture Store", result.merchant)
        assertEquals("3456", result.accountNumber)
        assertEquals("PP111222333", result.referenceNumber)
        assertEquals(55000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeMinimal() {
        // Given
        val sms = phonePeMinimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(25.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Tea Shop", result.merchant)
        assertEquals("PP789", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in minimal message
    }

    @Test
    fun parseUpiSms_parsesPhonePeWithDateAndBalance() {
        // Given
        val sms = phonePeWithDateAndBalanceSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(899.50, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("swiggy@paytm", result.upiId)
        assertEquals("Swiggy", result.merchant)
        assertEquals("6789", result.accountNumber)
        assertEquals("PP369258147", result.referenceNumber)
        assertEquals(7200.50, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeCreditWithDecimal() {
        // Given
        val sms = phonePeCreditWithDecimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2750.75, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("client@hdfcbank", result.upiId)
        assertEquals("Client", result.merchant)
        assertEquals("4321", result.accountNumber)
        assertEquals("PP987123654", result.referenceNumber)
        assertEquals(12750.75, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeTransferred() {
        // Given
        val sms = phonePeTransferredSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(3200.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("mom@oksbi", result.upiId)
        assertEquals("Mom", result.merchant)
        assertEquals("PP555444333", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPhonePeWithCommaAmount() {
        // Given
        val sms = phonePeWithCommaSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1999.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("amazon.pay@icici", result.upiId)
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("PP654987321", result.referenceNumber)
        assertEquals(5001.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== Paytm Parsing Tests ====================

    @Test
    fun parseUpiSms_parsesPaytmDebit() {
        // Given
        val sms = paytmDebitSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(350.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("bookmyshow@paytm", result.upiId)
        assertEquals("Bookmyshow", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("PTM123456789", result.referenceNumber)
        assertEquals(4500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmCredit() {
        // Given
        val sms = paytmCreditSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(750.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("client@okicici", result.upiId)
        assertEquals("Client", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("PTM987654321", result.referenceNumber)
        assertEquals(5250.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmP2P() {
        // Given
        val sms = paytmP2PSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("sister.name@ybl", result.upiId)
        assertEquals("Sister Name", result.merchant)
        assertEquals("PTM111222333", result.referenceNumber)
        assertEquals(4750.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmMerchant() {
        // Given
        val sms = paytmMerchantSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1299.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Reliance Digital", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("PTM444555666", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmLargeAmount() {
        // Given
        val sms = paytmLargeAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(35000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("furniture.mart@okaxis", result.upiId)
        assertEquals("Furniture Mart", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("PTM777888999", result.referenceNumber)
        assertEquals(65000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmMinimal() {
        // Given
        val sms = paytmMinimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(30.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Chai Stall", result.merchant)
        assertEquals("PTM321", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in minimal message
    }

    @Test
    fun parseUpiSms_parsesPaytmWithDateAndBalance() {
        // Given
        val sms = paytmWithDateAndBalanceSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(750.25, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("bigbasket@paytm", result.upiId)
        assertEquals("Bigbasket", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("PTM147852369", result.referenceNumber)
        assertEquals(9250.75, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmCreditWithDecimal() {
        // Given
        val sms = paytmCreditWithDecimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1850.50, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("freelance@hdfcbank", result.upiId)
        assertEquals("Freelance", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("PTM654321987", result.referenceNumber)
        assertEquals(11101.25, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmTransferred() {
        // Given
        val sms = paytmTransferredSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("dad@oksbi", result.upiId)
        assertEquals("Dad", result.merchant)
        assertEquals("PTM999111222", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmWithCommaAmount() {
        // Given
        val sms = paytmWithCommaSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1599.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("myntra@paytm", result.upiId)
        assertEquals("Myntra", result.merchant)
        assertEquals("PTM888777666", result.referenceNumber)
        assertEquals(7401.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmRefund() {
        // Given
        val sms = paytmRefundSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(499.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("amazon.pay@icici", result.upiId)
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("PTM555444333", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmP2MTransaction() {
        // Given
        val sms = paytmP2MSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1199.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("dominos@paytm", result.upiId)
        assertEquals("Dominos", result.merchant)
        assertEquals("PTM222333444", result.referenceNumber)
        assertEquals(5801.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmCreditWithFullDetails() {
        // Given
        val sms = paytmWithFullDetailsSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(4500.75, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("project.pay@axisbank", result.upiId)
        assertEquals("Project Pay", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("PTM963852741", result.referenceNumber)
        assertEquals(45500.75, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesPaytmAlternativeSender() {
        // Given
        val sms = paytmAlternativeSenderSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(899.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("zomato@paytm", result.upiId)
        assertEquals("Zomato", result.merchant)
        assertEquals("PTM123789456", result.referenceNumber)
        assertEquals(3101.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== Bank UPI Parsing Tests ====================

    @Test
    fun parseUpiSms_parsesHdfcUpi() {
        // Given
        val sms = hdfcUpiDebitSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(800.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("uber@paytm", result.upiId)
        assertEquals("Uber", result.merchant)
        assertEquals("4321", result.accountNumber)
        assertEquals("202401010012345", result.referenceNumber)
        assertEquals(12000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesSbiUpi() {
        // Given
        val sms = sbiUpiDebitSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1200.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("rapido@axisbank", result.upiId)
        assertEquals("Rapido", result.merchant)
        assertEquals("8765", result.accountNumber)
        assertEquals("402401010054321", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesIciciUpi() {
        // Given
        val sms = iciciUpiCreditSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(3500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("company@hdfcbank", result.upiId)
        assertEquals("Company", result.merchant)
        assertEquals("6543", result.accountNumber)
        assertEquals("ICI123456789012", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesAxisUpi() {
        // Given
        val sms = axisUpiDebitSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(650.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("olacabs@okaxis", result.upiId)
        assertEquals("Olacabs", result.merchant)
        assertEquals("3210", result.accountNumber)
        assertEquals("AX987654321098", result.referenceNumber)
        assertEquals(8500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesKotakUpi() {
        // Given
        val sms = kotakUpiSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("landlord@ybl", result.upiId)
        assertEquals("Landlord", result.merchant)
        assertEquals("7890", result.accountNumber)
        assertEquals("KTK123456789", result.referenceNumber)
        assertEquals(15000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesIdfcUpi() {
        // Given
        val sms = idbiUpiSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(999.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("netflix@icici", result.upiId)
        assertEquals("Netflix", result.merchant)
        assertEquals("4567", result.accountNumber)
        assertEquals("IDFC987654321", result.referenceNumber)
        assertEquals(6500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== HDFC UPI Comprehensive Tests ====================

    @Test
    fun parseUpiSms_parsesHdfcUpiCredit() {
        // Given
        val sms = hdfcUpiCreditSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(5500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("salary@company", result.upiId)
        assertEquals("Salary", result.merchant)
        assertEquals("1234", result.accountNumber)
        assertEquals("202601020098765", result.referenceNumber)
        assertEquals(45500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesHdfcUpiP2P() {
        // Given
        val sms = hdfcUpiP2PSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1200.50, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("friend@paytm", result.upiId)
        assertEquals("Friend", result.merchant)
        assertEquals("202601020012346", result.referenceNumber)
        assertEquals(11299.50, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesHdfcUpiMinimal() {
        // Given
        val sms = hdfcUpiMinimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(45.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Tea Shop", result.merchant)
        assertEquals("202601020000111", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in minimal message
    }

    @Test
    fun parseUpiSms_parsesHdfcUpiLargeAmount() {
        // Given
        val sms = hdfcUpiLargeAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(75000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("property@axisbank", result.upiId)
        assertEquals("Property", result.merchant)
        assertEquals("1234", result.accountNumber)
        assertEquals("202601020099999", result.referenceNumber)
        assertEquals(125000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesHdfcUpiWithCommaAmount() {
        // Given
        val sms = hdfcUpiWithCommaSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(3750.75, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("amazon.pay@icici", result.upiId)
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("1234", result.accountNumber)
        assertEquals("202601020054321", result.referenceNumber)
        assertEquals(21249.25, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== SBI UPI Comprehensive Tests ====================

    @Test
    fun parseUpiSms_parsesSbiUpiCredit() {
        // Given
        val sms = sbiUpiCreditSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(8250.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("freelance@hdfcbank", result.upiId)
        assertEquals("Freelance", result.merchant)
        assertEquals("5678", result.accountNumber)
        assertEquals("402601020087654", result.referenceNumber)
        assertEquals(28250.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesSbiUpiP2P() {
        // Given
        val sms = sbiUpiP2PSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2100.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("sister@ybl", result.upiId)
        assertEquals("Sister", result.merchant)
        assertEquals("402601020011111", result.referenceNumber)
        assertEquals(18900.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesSbiUpiMinimal() {
        // Given
        val sms = sbiUpiMinimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(65.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Snacks", result.merchant)
        assertEquals("402601020000222", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in minimal message
    }

    @Test
    fun parseUpiSms_parsesSbiUpiLargeAmount() {
        // Given
        val sms = sbiUpiLargeAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(95500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("jewellery.store@okaxis", result.upiId)
        assertEquals("Jewellery Store", result.merchant)
        assertEquals("5678", result.accountNumber)
        assertEquals("402601020099999", result.referenceNumber)
        assertEquals(204500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesSbiUpiRefund() {
        // Given
        val sms = sbiUpiRefundSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(1299.50, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("flipkart@axisbank", result.upiId)
        assertEquals("Flipkart", result.merchant)
        assertEquals("5678", result.accountNumber)
        assertEquals("402601020076543", result.referenceNumber)
        assertEquals(9799.50, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesSbiUpiWithDecimal() {
        // Given
        val sms = sbiUpiWithDecimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(4899.99, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("swiggy@paytm", result.upiId)
        assertEquals("Swiggy", result.merchant)
        assertEquals("5678", result.accountNumber)
        assertEquals("402601020055555", result.referenceNumber)
        assertEquals(14100.01, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== ICICI UPI Comprehensive Tests ====================

    @Test
    fun parseUpiSms_parsesIciciUpiDebit() {
        // Given
        val sms = iciciUpiDebitSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2750.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("zomato@paytm", result.upiId)
        assertEquals("Zomato", result.merchant)
        assertEquals("3456", result.accountNumber)
        assertEquals("ICI202601020012345", result.referenceNumber)
        assertEquals(17250.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesIciciUpiP2P() {
        // Given
        val sms = iciciUpiP2PSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("brother@ybl", result.upiId)
        assertEquals("Brother", result.merchant)
        assertEquals("ICI202601020098765", result.referenceNumber)
        assertEquals(25000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesIciciUpiMinimal() {
        // Given
        val sms = iciciUpiMinimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(55.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Coffee Shop", result.merchant)
        assertEquals("ICI202601020000333", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in minimal message
    }

    @Test
    fun parseUpiSms_parsesIciciUpiLargeAmount() {
        // Given
        val sms = iciciUpiLargeAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(125000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("car.dealer@hdfcbank", result.upiId)
        assertEquals("Car Dealer", result.merchant)
        assertEquals("3456", result.accountNumber)
        assertEquals("ICI202601020099999", result.referenceNumber)
        assertEquals(375000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesIciciUpiRefund() {
        // Given
        val sms = iciciUpiRefundSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2499.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("amazon.pay@icici", result.upiId)
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("3456", result.accountNumber)
        assertEquals("ICI202601020087654", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesIciciUpiWithDecimal() {
        // Given
        val sms = iciciUpiWithDecimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(6750.25, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("bigbasket@paytm", result.upiId)
        assertEquals("Bigbasket", result.merchant)
        assertEquals("3456", result.accountNumber)
        assertEquals("ICI202601020066666", result.referenceNumber)
        assertEquals(23249.75, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== Axis UPI Comprehensive Tests ====================

    @Test
    fun parseUpiSms_parsesAxisUpiCredit() {
        // Given
        val sms = axisUpiCreditSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(12500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("project@hdfcbank", result.upiId)
        assertEquals("Project", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("AX202601020087654", result.referenceNumber)
        assertEquals(52500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesAxisUpiP2P() {
        // Given
        val sms = axisUpiP2PSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(3500.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("mom@oksbi", result.upiId)
        assertEquals("Mom", result.merchant)
        assertEquals("AX202601020012345", result.referenceNumber)
        assertEquals(16500.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesAxisUpiMinimal() {
        // Given
        val sms = axisUpiMinimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(70.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Grocery", result.merchant)
        assertEquals("AX202601020000444", result.referenceNumber)
        assertEquals("UPI", result.paymentMethod)
        assertNull(result.upiId) // No VPA in minimal message
    }

    @Test
    fun parseUpiSms_parsesAxisUpiLargeAmount() {
        // Given
        val sms = axisUpiLargeAmountSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(215000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("furniture.mart@okaxis", result.upiId)
        assertEquals("Furniture Mart", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("AX202601020099999", result.referenceNumber)
        assertEquals(285000.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesAxisUpiRefund() {
        // Given
        val sms = axisUpiRefundSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(3799.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("myntra@paytm", result.upiId)
        assertEquals("Myntra", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("AX202601020076543", result.referenceNumber)
        assertEquals(23799.0, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    @Test
    fun parseUpiSms_parsesAxisUpiWithDecimal() {
        // Given
        val sms = axisUpiWithDecimalSms

        // When
        val result = parser.parseUpiSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(8999.50, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("petrol.pump@paytm", result.upiId)
        assertEquals("Petrol Pump", result.merchant)
        assertEquals("9876", result.accountNumber)
        assertEquals("AX202601020055555", result.referenceNumber)
        assertEquals(41000.50, result.balanceAfter)
        assertEquals("UPI", result.paymentMethod)
    }

    // ==================== Generic Bank SMS Parsing Tests ====================

    @Test
    fun parseSms_parsesGenericBankDebit() {
        // Given
        val sms = genericBankDebitSms

        // When
        val result = parser.parseSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Amazon", result.merchant)
        assertEquals("1234", result.accountNumber)
        assertEquals("123456789012", result.referenceNumber)
        assertEquals(25000.0, result.balanceAfter)
        assertNull(result.upiId) // Not a UPI transaction
        assertNull(result.paymentMethod) // Not a UPI transaction
    }

    @Test
    fun parseSms_parsesGenericBankCredit() {
        // Given
        val sms = genericBankCreditSms

        // When
        val result = parser.parseSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(15000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("5678", result.accountNumber)
        assertEquals("NEFT123456789", result.referenceNumber)
        assertEquals(40000.0, result.balanceAfter)
        assertNull(result.upiId)
        assertNull(result.paymentMethod)
    }

    @Test
    fun parseSms_parsesAtmWithdrawal() {
        // Given
        val sms = atmWithdrawalSms

        // When
        val result = parser.parseSms(sms)

        // Then
        assertNotNull(result)
        assertEquals(2000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("9999", result.accountNumber)
        assertEquals(23000.0, result.balanceAfter)
        assertEquals("Hdfc Atm Churchgate", result.location)
        assertNull(result.upiId)
        assertNull(result.paymentMethod)
    }

    @Test
    fun parseSms_routesToUpiParserForUpiSms() {
        // Given - UPI transaction
        val sms = googlePayDebitSms

        // When
        val result = parser.parseSms(sms)

        // Then - Should use UPI parser (has upiId and paymentMethod)
        assertNotNull(result)
        assertEquals("UPI", result!!.paymentMethod)
    }

    @Test
    fun parseSms_returnsNullForOtp() {
        // Given
        val sms = upiOtpSms

        // When
        val result = parser.parseSms(sms)

        // Then
        assertNull(result)
    }

    @Test
    fun parseSms_returnsNullForNonBankSms() {
        // Given
        val sms = createSms(
            address = "RANDOM",
            body = "Your package has been delivered. Thank you for shopping with us."
        )

        // When
        val result = parser.parseSms(sms)

        // Then
        assertNull(result)
    }

    // ==================== Deduplication Tests ====================

    @Test
    fun removeDuplicates_removesByReferenceNumber() {
        // Given - Same ref number
        val tx1 = parser.parseSms(googlePayDebitSms)!!.copy(
            smsId = 1L,
            referenceNumber = "REF123"
        )
        val tx2 = parser.parseSms(googlePayDebitSms)!!.copy(
            smsId = 2L,
            referenceNumber = "REF123"
        )

        // When
        val result = parser.removeDuplicates(listOf(tx1, tx2))

        // Then - Should keep only one
        assertEquals(1, result.size)
    }

    @Test
    fun removeDuplicates_removesBySimilarity() {
        // Given - Same amount, type, account, within 1 minute
        val now = System.currentTimeMillis()
        val tx1 = parser.parseSms(genericBankDebitSms)!!.copy(
            smsId = 1L,
            date = now,
            referenceNumber = null // No ref number
        )
        val tx2 = parser.parseSms(genericBankDebitSms)!!.copy(
            smsId = 2L,
            date = now + 30000, // 30 seconds later
            referenceNumber = null
        )

        // When
        val result = parser.removeDuplicates(listOf(tx1, tx2))

        // Then - Should keep only one
        assertEquals(1, result.size)
    }

    @Test
    fun removeDuplicates_keepsDifferentBalances() {
        // Given - Same amount but different balances (separate transactions)
        val now = System.currentTimeMillis()
        val tx1 = parser.parseSms(phonePeDebitSms)!!.copy(
            smsId = 1L,
            date = now,
            balanceAfter = 5000.0,
            referenceNumber = null
        )
        val tx2 = parser.parseSms(phonePeDebitSms)!!.copy(
            smsId = 2L,
            date = now + 30000,
            balanceAfter = 4500.0, // Different balance
            referenceNumber = null
        )

        // When
        val result = parser.removeDuplicates(listOf(tx1, tx2))

        // Then - Should keep both
        assertEquals(2, result.size)
    }

    @Test
    fun removeDuplicates_keepsTransactionsBeyondTimeWindow() {
        // Given - Same amount but 2 minutes apart
        val now = System.currentTimeMillis()
        val tx1 = parser.parseSms(googlePayDebitSms)!!.copy(
            smsId = 1L,
            date = now,
            referenceNumber = null
        )
        val tx2 = parser.parseSms(googlePayDebitSms)!!.copy(
            smsId = 2L,
            date = now + 120000, // 2 minutes later
            referenceNumber = null
        )

        // When
        val result = parser.removeDuplicates(listOf(tx1, tx2))

        // Then - Should keep both (beyond 1 minute window)
        assertEquals(2, result.size)
    }

    @Test
    fun removeDuplicates_sortsNewestFirst() {
        // Given - Transactions in random order
        val now = System.currentTimeMillis()
        val tx1 = parser.parseSms(googlePayDebitSms)!!.copy(
            smsId = 1L,
            date = now - 7200000, // 2 hours ago
            referenceNumber = "REF1"
        )
        val tx2 = parser.parseSms(phonePeDebitSms)!!.copy(
            smsId = 2L,
            date = now, // now
            referenceNumber = "REF2"
        )
        val tx3 = parser.parseSms(paytmDebitSms)!!.copy(
            smsId = 3L,
            date = now - 3600000, // 1 hour ago
            referenceNumber = "REF3"
        )

        // When
        val result = parser.removeDuplicates(listOf(tx1, tx3, tx2))

        // Then - Should be sorted newest first
        assertEquals(3, result.size)
        assertEquals(2L, result[0].smsId) // newest
        assertEquals(3L, result[1].smsId)
        assertEquals(1L, result[2].smsId) // oldest
    }

    @Test
    fun removeDuplicates_handlesEmptyList() {
        // Given
        val transactions = emptyList<com.example.kanakku.data.model.ParsedTransaction>()

        // When
        val result = parser.removeDuplicates(transactions)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun removeDuplicates_handlesSingleTransaction() {
        // Given
        val tx = parser.parseSms(googlePayDebitSms)!!

        // When
        val result = parser.removeDuplicates(listOf(tx))

        // Then
        assertEquals(1, result.size)
    }
}
