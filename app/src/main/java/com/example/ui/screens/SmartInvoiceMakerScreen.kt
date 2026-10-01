package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.*
import com.example.model.CustomerDetails
import com.example.model.InvoiceItem
import com.example.model.SellerDetails
import com.example.ui.components.AmbientLightingBackground
import com.example.ui.components.DarkFrostedSnackbarHost
import com.example.ui.components.FrostedGlassAlertDialog
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.util.InvoiceDocumentExporter
import com.example.util.InvoiceOcrParser
import com.example.util.JsonHelper
import com.example.util.MlKitTextExtractor
import com.example.util.ParsedOcrInvoice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartInvoiceMakerScreen(
    onBackClick: () -> Unit,
    initialInvoiceId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Repository Setup
    val repository = remember { com.example.data.repository.InvoiceRepository(context) }

    // Screen State: 0 = Dashboard & History, 1 = Form Builder (Create/Edit), 2 = OCR Review, 3 = Profiles
    var currentSubScreen by remember { mutableIntStateOf(0) }

    // Database Reactive Flows
    val invoicesList by repository.allInvoices.collectAsState(initial = emptyList())
    val sellerProfiles by repository.allSellerProfiles.collectAsState(initial = emptyList())
    val customerProfiles by repository.allCustomers.collectAsState(initial = emptyList())
    val productsList by repository.allProducts.collectAsState(initial = emptyList())

    // Invoice Form State
    var activeInvoiceId by remember { mutableStateOf("") }
    var sellerName by remember { mutableStateOf("") }
    var sellerOwner by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var sellerEmail by remember { mutableStateOf("") }
    var sellerAddress by remember { mutableStateOf("") }
    var sellerCity by remember { mutableStateOf("") }
    var sellerState by remember { mutableStateOf("") }
    var sellerPinCode by remember { mutableStateOf("") }
    var sellerGstin by remember { mutableStateOf("") }
    var sellerPan by remember { mutableStateOf("") }
    var sellerWebsite by remember { mutableStateOf("") }
    var sellerUpiId by remember { mutableStateOf("") }
    var sellerBankName by remember { mutableStateOf("") }
    var sellerAccountNo by remember { mutableStateOf("") }
    var sellerIfsc by remember { mutableStateOf("") }
    var sellerNotes by remember { mutableStateOf("") }

    var customerName by remember { mutableStateOf("") }
    var customerCompany by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var customerBilling by remember { mutableStateOf("") }
    var customerShipping by remember { mutableStateOf("") }
    var customerGstin by remember { mutableStateOf("") }
    var customerPan by remember { mutableStateOf("") }
    var customerNotes by remember { mutableStateOf("") }

    var invoiceNumber by remember { mutableStateOf("") }
    var invoiceDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("Unpaid") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var referenceNo by remember { mutableStateOf("") }
    var placeOfSupply by remember { mutableStateOf("") }
    var reverseCharge by remember { mutableStateOf(false) }
    var notesField by remember { mutableStateOf("") }
    var termsField by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("Classic") }

    val lineItems = remember { mutableStateListOf<InvoiceItem>() }
    var drawnSignaturePath by remember { mutableStateOf<String?>(null) }
    var signatureType by remember { mutableStateOf("none") }
    var isOcrUncertain by remember { mutableStateOf(false) }
    var originalBillImageUri by remember { mutableStateOf<String?>(null) }

    var ocrReviewData by remember { mutableStateOf<ParsedOcrInvoice?>(null) }
    var isOcrExtracting by remember { mutableStateOf(false) }

    // Search and Filters for History
    var historySearchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") } // All, Paid, Unpaid, Partially Paid, Overdue
    var sortOrder by remember { mutableStateOf("Newest") } // Newest, Oldest, Amount High, Amount Low

    // Draft Recovery or Initial Invoice Loading
    LaunchedEffect(initialInvoiceId) {
        if (!initialInvoiceId.isNullOrBlank()) {
            val invoice = repository.getInvoiceById(initialInvoiceId)
            if (invoice != null) {
                activeInvoiceId = invoice.id
                invoiceNumber = invoice.invoiceNumber
                invoiceDate = invoice.invoiceDate
                dueDate = invoice.dueDate
                paymentStatus = invoice.paymentStatus
                paymentMethod = invoice.paymentMethod
                referenceNo = invoice.referenceNo
                placeOfSupply = invoice.placeOfSupply
                reverseCharge = invoice.reverseCharge
                notesField = invoice.notes
                termsField = invoice.terms
                selectedTemplate = invoice.templateName
                drawnSignaturePath = invoice.signaturePath
                signatureType = invoice.signatureType
                originalBillImageUri = invoice.originalBillImageUri

                val dSeller = JsonHelper.fromSellerJson(invoice.sellerDetailsJson)
                sellerName = dSeller.name
                sellerOwner = dSeller.ownerName
                sellerPhone = dSeller.phone
                sellerEmail = dSeller.email
                sellerAddress = dSeller.address
                sellerCity = dSeller.city
                sellerState = dSeller.state
                sellerPinCode = dSeller.pinCode
                sellerGstin = dSeller.gstin
                sellerPan = dSeller.pan
                sellerWebsite = dSeller.website
                sellerUpiId = dSeller.upiId
                sellerBankName = dSeller.bankName
                sellerAccountNo = dSeller.accountNumber
                sellerIfsc = dSeller.ifsc
                sellerNotes = dSeller.additionalNotes

                val dCust = JsonHelper.fromCustomerJson(invoice.customerDetailsJson)
                customerName = dCust.name
                customerCompany = dCust.companyName
                customerPhone = dCust.phone
                customerEmail = dCust.email
                customerBilling = dCust.billingAddress
                customerShipping = dCust.shippingAddress
                customerGstin = dCust.gstin
                customerPan = dCust.pan
                customerNotes = dCust.notes

                lineItems.clear()
                lineItems.addAll(JsonHelper.fromItemsJson(invoice.itemsJson))
                currentSubScreen = 1 // Open editor
            }
        } else {
            val draft = repository.getDraft()
            if (draft != null) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Restored draft invoice from last session! 📝")
                    activeInvoiceId = draft.id
                    invoiceNumber = draft.invoiceNumber
                    invoiceDate = draft.invoiceDate
                    dueDate = draft.dueDate
                    paymentStatus = draft.paymentStatus
                    paymentMethod = draft.paymentMethod
                    referenceNo = draft.referenceNo
                    placeOfSupply = draft.placeOfSupply
                    reverseCharge = draft.reverseCharge
                    notesField = draft.notes
                    termsField = draft.terms
                    selectedTemplate = draft.templateName
                    drawnSignaturePath = draft.signaturePath
                    signatureType = draft.signatureType
                    originalBillImageUri = draft.originalBillImageUri

                    val dSeller = JsonHelper.fromSellerJson(draft.sellerDetailsJson)
                    sellerName = dSeller.name
                    sellerOwner = dSeller.ownerName
                    sellerPhone = dSeller.phone
                    sellerEmail = dSeller.email
                    sellerAddress = dSeller.address
                    sellerCity = dSeller.city
                    sellerState = dSeller.state
                    sellerPinCode = dSeller.pinCode
                    sellerGstin = dSeller.gstin
                    sellerPan = dSeller.pan
                    sellerWebsite = dSeller.website
                    sellerUpiId = dSeller.upiId
                    sellerBankName = dSeller.bankName
                    sellerAccountNo = dSeller.accountNumber
                    sellerIfsc = dSeller.ifsc
                    sellerNotes = dSeller.additionalNotes

                    val dCust = JsonHelper.fromCustomerJson(draft.customerDetailsJson)
                    customerName = dCust.name
                    customerCompany = dCust.companyName
                    customerPhone = dCust.phone
                    customerEmail = dCust.email
                    customerBilling = dCust.billingAddress
                    customerShipping = dCust.shippingAddress
                    customerGstin = dCust.gstin
                    customerPan = dCust.pan
                    customerNotes = dCust.notes

                    lineItems.clear()
                    lineItems.addAll(JsonHelper.fromItemsJson(draft.itemsJson))
                    currentSubScreen = 1
                }
            } else {
                // Load default seller profile if any
                val defSeller = repository.getDefaultSellerProfile()
                if (defSeller != null) {
                    sellerName = defSeller.name
                    sellerOwner = defSeller.ownerName
                    sellerPhone = defSeller.phone
                    sellerEmail = defSeller.email
                    sellerAddress = defSeller.address
                    sellerCity = defSeller.city
                    sellerState = defSeller.state
                    sellerPinCode = defSeller.pinCode
                    sellerGstin = defSeller.gstin
                    sellerPan = defSeller.pan
                    sellerWebsite = defSeller.website
                    sellerUpiId = defSeller.upiId
                    sellerBankName = defSeller.bankName
                    sellerAccountNo = defSeller.accountNumber
                    sellerIfsc = defSeller.ifsc
                    sellerNotes = defSeller.additionalNotes
                }
            }
        }
    }

    // Auto-save Draft function
    fun autoSaveDraft() {
        coroutineScope.launch {
            val sellerDetails = SellerDetails(
                name = sellerName, ownerName = sellerOwner, phone = sellerPhone, email = sellerEmail,
                address = sellerAddress, city = sellerCity, state = sellerState, pinCode = sellerPinCode,
                gstin = sellerGstin, pan = sellerPan, website = sellerWebsite, upiId = sellerUpiId,
                bankName = sellerBankName, accountNumber = sellerAccountNo, ifsc = sellerIfsc, additionalNotes = sellerNotes
            )
            val customerDetails = CustomerDetails(
                name = customerName, companyName = customerCompany, phone = customerPhone, email = customerEmail,
                billingAddress = customerBilling, shippingAddress = customerShipping, gstin = customerGstin,
                pan = customerPan, notes = customerNotes
            )
            val draft = InvoiceDraftEntity(
                invoiceNumber = invoiceNumber, invoiceDate = invoiceDate, dueDate = dueDate,
                paymentStatus = paymentStatus, paymentMethod = paymentMethod, referenceNo = referenceNo,
                placeOfSupply = placeOfSupply, reverseCharge = reverseCharge, notes = notesField,
                terms = termsField, templateName = selectedTemplate, signaturePath = drawnSignaturePath,
                signatureType = signatureType, originalBillImageUri = originalBillImageUri,
                sellerDetailsJson = JsonHelper.toJson(sellerDetails),
                customerDetailsJson = JsonHelper.toJson(customerDetails),
                itemsJson = JsonHelper.toJson(lineItems.toList())
            )
            repository.saveDraft(draft)
        }
    }

    // ML Kit Scanner activity result
    val ocrImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isOcrExtracting = true
            originalBillImageUri = uri.toString()
            coroutineScope.launch {
                val fullOcrText = MlKitTextExtractor.extractTextFromUri(context, uri)
                if (fullOcrText.isNotBlank()) {
                    val parsed = InvoiceOcrParser.parseOcrText(fullOcrText)
                    ocrReviewData = parsed
                    currentSubScreen = 2 // Go to OCR Review Screen
                } else {
                    snackbarHostState.showSnackbar("Failed to detect readable text from image. 📸")
                }
                isOcrExtracting = false
            }
        }
    }

    // Mathematical Totals Calculation Engine
    val subtotal = lineItems.sumOf { it.quantity * it.unitPrice }
    val totalDiscount = lineItems.sumOf { (it.quantity * it.unitPrice) * (it.discountPercent / 100.0) + it.discountAmount }
    val taxableAmount = subtotal - totalDiscount
    
    // Tax Rates computation
    val cgst = lineItems.sumOf {
        val lineTaxable = (it.quantity * it.unitPrice) - ((it.quantity * it.unitPrice) * (it.discountPercent / 100.0) + it.discountAmount)
        lineTaxable * (it.cgstRatePercent / 100.0)
    }
    val sgst = lineItems.sumOf {
        val lineTaxable = (it.quantity * it.unitPrice) - ((it.quantity * it.unitPrice) * (it.discountPercent / 100.0) + it.discountAmount)
        lineTaxable * (it.sgstRatePercent / 100.0)
    }
    val igst = lineItems.sumOf {
        val lineTaxable = (it.quantity * it.unitPrice) - ((it.quantity * it.unitPrice) * (it.discountPercent / 100.0) + it.discountAmount)
        lineTaxable * (it.igstRatePercent / 100.0)
    }
    val cess = lineItems.sumOf {
        val lineTaxable = (it.quantity * it.unitPrice) - ((it.quantity * it.unitPrice) * (it.discountPercent / 100.0) + it.discountAmount)
        lineTaxable * (it.cessRatePercent / 100.0)
    }
    
    val additionalChargesVal = 0.0
    val rawGrandTotal = taxableAmount + cgst + sgst + igst + cess + additionalChargesVal
    val roundedGrandTotal = Math.round(rawGrandTotal).toDouble()
    val roundOff = roundedGrandTotal - rawGrandTotal

    var amountPaidVal by remember { mutableStateOf("0.00") }
    val amountPaidDouble = amountPaidVal.toDoubleOrNull() ?: 0.00
    val balanceDue = (roundedGrandTotal - amountPaidDouble).coerceAtLeast(0.0)

    AmbientLightingBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { DarkFrostedSnackbarHost(snackbarHostState) },
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentSubScreen) {
                                1 -> if (activeInvoiceId.isBlank()) "Create Invoice" else "Edit Invoice"
                                2 -> "Review Scanned Bill"
                                3 -> "Saved Profiles"
                                else -> "Smart Invoice Maker"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    },
                    navigationIcon = {
                        if (currentSubScreen > 0) {
                            IconButton(
                                onClick = {
                                    currentSubScreen = 0
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentSubScreen) {
                    0 -> DashboardAndHistoryScreen(
                        invoices = invoicesList,
                        searchQuery = historySearchQuery,
                        onSearchChange = { historySearchQuery = it },
                        filterStatus = filterStatus,
                        onFilterChange = { filterStatus = it },
                        sortOrder = sortOrder,
                        onSortChange = { sortOrder = it },
                        onCreateInvoiceClick = {
                            // Reset state for new Invoice
                            activeInvoiceId = ""
                            invoiceNumber = ""
                            invoiceDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            paymentStatus = "Unpaid"
                            paymentMethod = "Cash"
                            referenceNo = ""
                            placeOfSupply = ""
                            reverseCharge = false
                            notesField = ""
                            termsField = ""
                            lineItems.clear()
                            drawnSignaturePath = null
                            signatureType = "none"
                            amountPaidVal = "0.00"
                            originalBillImageUri = null
                            isOcrUncertain = false

                            // Reset seller states
                            sellerName = ""
                            sellerOwner = ""
                            sellerPhone = ""
                            sellerEmail = ""
                            sellerAddress = ""
                            sellerCity = ""
                            sellerState = ""
                            sellerPinCode = ""
                            sellerGstin = ""
                            sellerPan = ""
                            sellerWebsite = ""
                            sellerUpiId = ""
                            sellerBankName = ""
                            sellerAccountNo = ""
                            sellerIfsc = ""
                            sellerNotes = ""

                            // Reset customer states
                            customerName = ""
                            customerCompany = ""
                            customerPhone = ""
                            customerEmail = ""
                            customerBilling = ""
                            customerShipping = ""
                            customerGstin = ""
                            customerPan = ""
                            customerNotes = ""

                            // Load default seller profile asynchronously if exists
                            coroutineScope.launch {
                                val defSeller = repository.getDefaultSellerProfile()
                                if (defSeller != null) {
                                    sellerName = defSeller.name
                                    sellerOwner = defSeller.ownerName
                                    sellerPhone = defSeller.phone
                                    sellerEmail = defSeller.email
                                    sellerAddress = defSeller.address
                                    sellerCity = defSeller.city
                                    sellerState = defSeller.state
                                    sellerPinCode = defSeller.pinCode
                                    sellerGstin = defSeller.gstin
                                    sellerPan = defSeller.pan
                                    sellerWebsite = defSeller.website
                                    sellerUpiId = defSeller.upiId
                                    sellerBankName = defSeller.bankName
                                    sellerAccountNo = defSeller.accountNumber
                                    sellerIfsc = defSeller.ifsc
                                    sellerNotes = defSeller.additionalNotes
                                }
                            }

                            currentSubScreen = 1
                        },
                        onScanBillClick = {
                            ocrImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onSavedProfilesClick = { currentSubScreen = 3 },
                        onEditInvoice = { inv ->
                            activeInvoiceId = inv.id
                            invoiceNumber = inv.invoiceNumber
                            invoiceDate = inv.invoiceDate
                            dueDate = inv.dueDate
                            paymentStatus = inv.paymentStatus
                            paymentMethod = inv.paymentMethod
                            referenceNo = inv.referenceNo
                            placeOfSupply = inv.placeOfSupply
                            reverseCharge = inv.reverseCharge
                            notesField = inv.notes
                            termsField = inv.terms
                            selectedTemplate = inv.templateName
                            drawnSignaturePath = inv.signaturePath
                            signatureType = inv.signatureType
                            originalBillImageUri = inv.originalBillImageUri
                            amountPaidVal = inv.amountPaid.toString()

                            val dSeller = JsonHelper.fromSellerJson(inv.sellerDetailsJson)
                            sellerName = dSeller.name
                            sellerOwner = dSeller.ownerName
                            sellerPhone = dSeller.phone
                            sellerEmail = dSeller.email
                            sellerAddress = dSeller.address
                            sellerCity = dSeller.city
                            sellerState = dSeller.state
                            sellerPinCode = dSeller.pinCode
                            sellerGstin = dSeller.gstin
                            sellerPan = dSeller.pan
                            sellerWebsite = dSeller.website
                            sellerUpiId = dSeller.upiId
                            sellerBankName = dSeller.bankName
                            sellerAccountNo = dSeller.accountNumber
                            sellerIfsc = dSeller.ifsc
                            sellerNotes = dSeller.additionalNotes

                            val dCust = JsonHelper.fromCustomerJson(inv.customerDetailsJson)
                            customerName = dCust.name
                            customerCompany = dCust.companyName
                            customerPhone = dCust.phone
                            customerEmail = dCust.email
                            customerBilling = dCust.billingAddress
                            customerShipping = dCust.shippingAddress
                            customerGstin = dCust.gstin
                            customerPan = dCust.pan
                            customerNotes = dCust.notes

                            lineItems.clear()
                            lineItems.addAll(JsonHelper.fromItemsJson(inv.itemsJson))
                            currentSubScreen = 1
                        },
                        onDeleteInvoice = { inv ->
                            coroutineScope.launch {
                                repository.deleteInvoiceById(inv.id)
                                snackbarHostState.showSnackbar("Deleted Invoice ${inv.invoiceNumber} successfully 🗑️")
                            }
                        },
                        onDuplicateInvoice = { inv ->
                            coroutineScope.launch {
                                val duplicated = inv.copy(
                                    id = UUID.randomUUID().toString(),
                                    invoiceNumber = "${inv.invoiceNumber}-DUP",
                                    createdAt = System.currentTimeMillis()
                                )
                                repository.saveInvoice(duplicated)
                                snackbarHostState.showSnackbar("Duplicated Invoice ${inv.invoiceNumber} successfully! 📑")
                            }
                        },
                        onShareInvoice = { inv, type ->
                            coroutineScope.launch {
                                val uri = when (type) {
                                    "TXT" -> InvoiceDocumentExporter.generateTxtInvoice(context, inv)
                                    "DOC" -> InvoiceDocumentExporter.generateDocInvoice(context, inv)
                                    else -> InvoiceDocumentExporter.generatePdfInvoice(context, inv)
                                }
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        this.type = when (type) {
                                            "TXT" -> "text/plain"
                                            "DOC" -> "application/msword"
                                            else -> "application/pdf"
                                        }
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Invoice ($type)"))
                                } else {
                                    snackbarHostState.showSnackbar("Failed to share file.")
                                }
                            }
                        },
                        onPrintInvoice = { inv ->
                            coroutineScope.launch {
                                val uri = InvoiceDocumentExporter.generatePdfInvoice(context, inv)
                                if (uri != null) {
                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                                    if (printManager != null) {
                                        try {
                                            val printAdapter = com.example.util.PdfPrintDocumentAdapter(context, uri)
                                            printManager.print("GTool X Invoice - ${inv.invoiceNumber}", printAdapter, null)
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Printing failed.")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Printing service not available.")
                                    }
                                }
                            }
                        }
                    )
                    1 -> InvoiceFormBuilderScreen(
                        sellerName = sellerName, onSellerNameChange = { sellerName = it; autoSaveDraft() },
                        sellerOwner = sellerOwner, onSellerOwnerChange = { sellerOwner = it; autoSaveDraft() },
                        sellerPhone = sellerPhone, onSellerPhoneChange = { sellerPhone = it; autoSaveDraft() },
                        sellerEmail = sellerEmail, onSellerEmailChange = { sellerEmail = it; autoSaveDraft() },
                        sellerAddress = sellerAddress, onSellerAddressChange = { sellerAddress = it; autoSaveDraft() },
                        sellerCity = sellerCity, onSellerCityChange = { sellerCity = it; autoSaveDraft() },
                        sellerState = sellerState, onSellerStateChange = { sellerState = it; autoSaveDraft() },
                        sellerPinCode = sellerPinCode, onSellerPinCodeChange = { sellerPinCode = it; autoSaveDraft() },
                        sellerGstin = sellerGstin, onSellerGstinChange = { sellerGstin = it; autoSaveDraft() },
                        sellerPan = sellerPan, onSellerPanChange = { sellerPan = it; autoSaveDraft() },
                        sellerWebsite = sellerWebsite, onSellerWebsiteChange = { sellerWebsite = it; autoSaveDraft() },
                        sellerUpiId = sellerUpiId, onSellerUpiIdChange = { sellerUpiId = it; autoSaveDraft() },
                        sellerBankName = sellerBankName, onSellerBankNameChange = { sellerBankName = it; autoSaveDraft() },
                        sellerAccountNo = sellerAccountNo, onSellerAccountNoChange = { sellerAccountNo = it; autoSaveDraft() },
                        sellerIfsc = sellerIfsc, onSellerIfscChange = { sellerIfsc = it; autoSaveDraft() },
                        sellerNotes = sellerNotes, onSellerNotesChange = { sellerNotes = it; autoSaveDraft() },
                        customerName = customerName, onCustomerNameChange = { customerName = it; autoSaveDraft() },
                        customerCompany = customerCompany, onCustomerCompanyChange = { customerCompany = it; autoSaveDraft() },
                        customerPhone = customerPhone, onCustomerPhoneChange = { customerPhone = it; autoSaveDraft() },
                        customerEmail = customerEmail, onCustomerEmailChange = { customerEmail = it; autoSaveDraft() },
                        customerBilling = customerBilling, onCustomerBillingChange = { customerBilling = it; autoSaveDraft() },
                        customerShipping = customerShipping, onCustomerShippingChange = { customerShipping = it; autoSaveDraft() },
                        customerGstin = customerGstin, onCustomerGstinChange = { customerGstin = it; autoSaveDraft() },
                        customerPan = customerPan, onCustomerPanChange = { customerPan = it; autoSaveDraft() },
                        customerNotes = customerNotes, onCustomerNotesChange = { customerNotes = it; autoSaveDraft() },
                        invoiceNumber = invoiceNumber, onInvoiceNumberChange = { invoiceNumber = it; autoSaveDraft() },
                        invoiceDate = invoiceDate, onInvoiceDateChange = { invoiceDate = it; autoSaveDraft() },
                        dueDate = dueDate, onDueDateChange = { dueDate = it; autoSaveDraft() },
                        paymentStatus = paymentStatus, onPaymentStatusChange = { paymentStatus = it; autoSaveDraft() },
                        paymentMethod = paymentMethod, onPaymentMethodChange = { paymentMethod = it; autoSaveDraft() },
                        referenceNo = referenceNo, onReferenceNoChange = { referenceNo = it; autoSaveDraft() },
                        placeOfSupply = placeOfSupply, onPlaceOfSupplyChange = { placeOfSupply = it; autoSaveDraft() },
                        reverseCharge = reverseCharge, onReverseChargeChange = { reverseCharge = it; autoSaveDraft() },
                        notesField = notesField, onNotesFieldChange = { notesField = it; autoSaveDraft() },
                        termsField = termsField, onTermsFieldChange = { termsField = it; autoSaveDraft() },
                        selectedTemplate = selectedTemplate, onTemplateChange = { selectedTemplate = it; autoSaveDraft() },
                        lineItems = lineItems,
                        subtotal = subtotal,
                        totalDiscount = totalDiscount,
                        taxableAmount = taxableAmount,
                        cgst = cgst,
                        sgst = sgst,
                        igst = igst,
                        cess = cess,
                        roundOff = roundOff,
                        grandTotal = roundedGrandTotal,
                        amountPaid = amountPaidVal,
                        onAmountPaidChange = { amountPaidVal = it; autoSaveDraft() },
                        balanceDue = balanceDue,
                        drawnSignaturePath = drawnSignaturePath,
                        signatureType = signatureType,
                        onSignatureDrawSave = { path ->
                            drawnSignaturePath = path
                            signatureType = "draw"
                            autoSaveDraft()
                        },
                        onSavedSellers = sellerProfiles,
                        onSavedCustomers = customerProfiles,
                        onSavedProducts = productsList,
                        onSaveInvoice = {
                            coroutineScope.launch {
                                val effectiveSeller = sellerName.ifBlank { "My Business" }
                                val effectiveCustomer = customerName.ifBlank { "Valued Customer" }
                                val effectiveInvoiceNo = invoiceNumber.ifBlank { "INV-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}" }
                                val effectiveDate = invoiceDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

                                if (lineItems.isEmpty()) {
                                    lineItems.add(
                                        InvoiceItem(
                                            name = "General Items / Services",
                                            quantity = 1.0,
                                            unitPrice = roundedGrandTotal.coerceAtLeast(0.0),
                                            lineTotal = roundedGrandTotal.coerceAtLeast(0.0)
                                        )
                                    )
                                }

                                val id = if (activeInvoiceId.isBlank()) UUID.randomUUID().toString() else activeInvoiceId
                                val sellerDetails = SellerDetails(
                                    name = effectiveSeller, ownerName = sellerOwner, phone = sellerPhone, email = sellerEmail,
                                    address = sellerAddress, city = sellerCity, state = sellerState, pinCode = sellerPinCode,
                                    gstin = sellerGstin, pan = sellerPan, website = sellerWebsite, upiId = sellerUpiId,
                                    bankName = sellerBankName, accountNumber = sellerAccountNo, ifsc = sellerIfsc, additionalNotes = sellerNotes
                                )
                                val customerDetails = CustomerDetails(
                                    name = effectiveCustomer, companyName = customerCompany, phone = customerPhone, email = customerEmail,
                                    billingAddress = customerBilling, shippingAddress = customerShipping, gstin = customerGstin,
                                    pan = customerPan, notes = customerNotes
                                )
                                val entity = InvoiceEntity(
                                    id = id,
                                    invoiceNumber = effectiveInvoiceNo,
                                    invoiceDate = effectiveDate,
                                    dueDate = dueDate,
                                    paymentStatus = paymentStatus,
                                    paymentMethod = paymentMethod,
                                    referenceNo = referenceNo,
                                    placeOfSupply = placeOfSupply,
                                    reverseCharge = reverseCharge,
                                    sellerDetailsJson = JsonHelper.toJson(sellerDetails),
                                    customerDetailsJson = JsonHelper.toJson(customerDetails),
                                    itemsJson = JsonHelper.toJson(lineItems.toList()),
                                    notes = notesField,
                                    terms = termsField,
                                    subtotal = subtotal,
                                    totalDiscount = totalDiscount,
                                    taxableAmount = taxableAmount,
                                    cgst = cgst,
                                    sgst = sgst,
                                    igst = igst,
                                    cess = cess,
                                    roundOff = roundOff,
                                    grandTotal = roundedGrandTotal,
                                    amountPaid = amountPaidDouble,
                                    balanceDue = balanceDue,
                                    signaturePath = drawnSignaturePath,
                                    signatureType = signatureType,
                                    source = if (originalBillImageUri != null) "OCR" else "Manual",
                                    templateName = selectedTemplate,
                                    isOcrUncertain = isOcrUncertain,
                                    originalBillImageUri = originalBillImageUri
                                )
                                repository.saveInvoice(entity)
                                
                                // Auto-export to Documents/GTOOL X/Invoices/
                                InvoiceDocumentExporter.generatePdfInvoice(context, entity)

                                if (sellerName.isNotBlank() && sellerProfiles.none { it.name.equals(sellerName, ignoreCase = true) }) {
                                    repository.saveSellerProfile(
                                        SellerProfileEntity(
                                            id = UUID.randomUUID().toString(),
                                            name = sellerName, ownerName = sellerOwner, phone = sellerPhone, email = sellerEmail,
                                            address = sellerAddress, city = sellerCity, state = sellerState, pinCode = sellerPinCode,
                                            gstin = sellerGstin, pan = sellerPan, website = sellerWebsite, upiId = sellerUpiId,
                                            bankName = sellerBankName, accountNumber = sellerAccountNo, ifsc = sellerIfsc, additionalNotes = sellerNotes
                                        )
                                    )
                                }
                                if (customerName.isNotBlank() && customerProfiles.none { it.name.equals(customerName, ignoreCase = true) }) {
                                    repository.saveCustomer(
                                        CustomerProfileEntity(
                                            id = UUID.randomUUID().toString(),
                                            name = customerName, companyName = customerCompany, phone = customerPhone, email = customerEmail,
                                            billingAddress = customerBilling, shippingAddress = customerShipping, gstin = customerGstin,
                                            pan = customerPan, notes = customerNotes
                                        )
                                    )
                                }

                                repository.deleteDraft()
                                currentSubScreen = 0
                                snackbarHostState.showSnackbar("Invoice saved to Documents/GTOOL X/Invoices! 📑")
                            }
                        }
                    )
                    2 -> OCRReviewScreen(
                        parsedInvoice = ocrReviewData!!,
                        onAccept = { reviewed ->
                            // Map values from OCR Review to Invoice Form
                            activeInvoiceId = ""
                            invoiceNumber = reviewed.invoiceNumber
                            invoiceDate = reviewed.invoiceDate
                            dueDate = reviewed.dueDate
                            paymentStatus = "Unpaid"
                            paymentMethod = reviewed.seller.upiId.let { if (it.isNotBlank()) "UPI" else "Cash" }
                            referenceNo = ""
                            placeOfSupply = ""
                            reverseCharge = false
                            notesField = "Extracted from Scanned Bill"
                            termsField = ""
                            selectedTemplate = "GST-focused"
                            drawnSignaturePath = null
                            signatureType = "none"

                            sellerName = reviewed.seller.name
                            sellerOwner = reviewed.seller.ownerName
                            sellerPhone = reviewed.seller.phone
                            sellerEmail = reviewed.seller.email
                            sellerAddress = reviewed.seller.address
                            sellerCity = reviewed.seller.city
                            sellerState = reviewed.seller.state
                            sellerPinCode = reviewed.seller.pinCode
                            sellerGstin = reviewed.seller.gstin
                            sellerPan = reviewed.seller.pan
                            sellerWebsite = reviewed.seller.website
                            sellerUpiId = reviewed.seller.upiId
                            sellerBankName = reviewed.seller.bankName
                            sellerAccountNo = reviewed.seller.accountNumber
                            sellerIfsc = reviewed.seller.ifsc
                            sellerNotes = reviewed.seller.additionalNotes

                            customerName = reviewed.customer.name
                            customerCompany = reviewed.customer.companyName
                            customerPhone = reviewed.customer.phone
                            customerEmail = reviewed.customer.email
                            customerBilling = reviewed.customer.billingAddress
                            customerShipping = reviewed.customer.shippingAddress
                            customerGstin = reviewed.customer.gstin
                            customerPan = reviewed.customer.pan
                            customerNotes = reviewed.customer.notes

                            lineItems.clear()
                            lineItems.addAll(reviewed.items)
                            amountPaidVal = "0.00"
                            isOcrUncertain = reviewed.isOcrUncertain

                            currentSubScreen = 1 // Go to form builder
                        },
                        onCancel = {
                            currentSubScreen = 0
                        }
                    )
                    3 -> SavedProfilesScreen(
                        sellerProfiles = sellerProfiles,
                        customerProfiles = customerProfiles,
                        productsList = productsList,
                        onSaveSeller = { seller ->
                            coroutineScope.launch {
                                repository.saveSellerProfile(seller)
                                snackbarHostState.showSnackbar("Seller Profile Saved! 💼")
                            }
                        },
                        onDeleteSeller = { seller ->
                            coroutineScope.launch {
                                repository.deleteSellerProfileById(seller.id)
                            }
                        },
                        onSaveCustomer = { customer ->
                            coroutineScope.launch {
                                repository.saveCustomer(customer)
                                snackbarHostState.showSnackbar("Customer Profile Saved! 👥")
                            }
                        },
                        onDeleteCustomer = { customer ->
                            coroutineScope.launch {
                                repository.deleteCustomerById(customer.id)
                            }
                        },
                        onSaveProduct = { prod ->
                            coroutineScope.launch {
                                repository.saveProduct(prod)
                                snackbarHostState.showSnackbar("Product Saved successfully! 📦")
                            }
                        },
                        onDeleteProduct = { prod ->
                            coroutineScope.launch {
                                repository.deleteProductById(prod.id)
                            }
                        }
                    )
                }

                if (isOcrExtracting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AmberWarm)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Processing Bill with ML Kit OCR...", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 0: DASHBOARD & HISTORY VIEW
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAndHistoryScreen(
    invoices: List<InvoiceEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterChange: (String) -> Unit,
    sortOrder: String,
    onSortChange: (String) -> Unit,
    onCreateInvoiceClick: () -> Unit,
    onScanBillClick: () -> Unit,
    onSavedProfilesClick: () -> Unit,
    onEditInvoice: (InvoiceEntity) -> Unit,
    onDeleteInvoice: (InvoiceEntity) -> Unit,
    onDuplicateInvoice: (InvoiceEntity) -> Unit,
    onShareInvoice: (InvoiceEntity, String) -> Unit,
    onPrintInvoice: (InvoiceEntity) -> Unit
) {
    val scrollState = rememberScrollState()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Financial Metrics Calculation
    val totalInvoiced = invoices.sumOf { it.grandTotal }
    val paidAmount = invoices.sumOf { if (it.paymentStatus == "Paid") it.grandTotal else it.amountPaid }
    val pendingInvoices = invoices.filter { it.paymentStatus != "Paid" }
    val pendingAmount = invoices.sumOf { it.balanceDue }
    val pendingCount = pendingInvoices.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Action Row Buttons directly at the top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateInvoiceClick,
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Invoice", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }

            Button(
                onClick = onScanBillClick,
                colors = ButtonDefaults.buttonColors(containerColor = SyncGreen),
                modifier = Modifier
                    .weight(1.1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Rounded.DocumentScanner, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan OCR Bill", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }

        // 2. Quick Stats / Financial Overview Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            borderColorList = listOf(Color(0x60FFFFFF), Color(0x15FFFFFF)),
            ambientGlowColor = AmberWarm
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AmberWarm.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = AmberWarm,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Financial Overview",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Text(
                        text = "${invoices.size} Invoices",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Invoiced Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Total Invoiced",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹ %.0f".format(totalInvoiced),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AmberWarm,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }

                    // Paid Amount Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Paid Collected",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹ %.0f".format(paidAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SyncGreen,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }

                    // Pending Invoices Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "$pendingCount Pending",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹ %.0f".format(pendingAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (pendingAmount > 0) Color(0xFFFF6B6B) else TextPrimary,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Configuration & Saved Profiles shortcut
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x18FFFFFF))
                .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                .clickable { onSavedProfilesClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x25FFB87E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBox,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Saved Profiles & Products",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Manage Businesses, Customers & Catalog",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 4. Search & Filter History Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Invoices",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            )

            if (invoices.isNotEmpty()) {
                Text(
                    text = "${invoices.size} total",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by Invoice No / Customer...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x14FFFFFF),
                unfocusedContainerColor = Color(0x0AFFFFFF),
                focusedBorderColor = AmberWarm.copy(alpha = 0.6f),
                unfocusedBorderColor = GlassStroke,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Horizontal filter pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Paid", "Unpaid", "Overdue").forEach { status ->
                val isSelected = filterStatus == status
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) AmberWarm.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                        .border(1.dp, if (isSelected) AmberWarm else Color(0x20FFFFFF), CircleShape)
                        .clickable { onFilterChange(status) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) AmberWarm else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // History items calculation
        val filteredInvoices = invoices.filter { inv ->
            val cust = JsonHelper.fromCustomerJson(inv.customerDetailsJson)
            val matchesQuery = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    cust.name.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (filterStatus) {
                "All" -> true
                "Paid" -> inv.paymentStatus == "Paid"
                "Unpaid" -> inv.paymentStatus == "Unpaid"
                "Overdue" -> inv.dueDate < today && inv.paymentStatus != "Paid"
                else -> true
            }
            matchesQuery && matchesStatus
        }

        if (filteredInvoices.isEmpty()) {
            // Rich Centered Empty State Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(24.dp),
                borderColorList = listOf(Color(0x40FFFFFF), Color(0x10FFFFFF)),
                ambientGlowColor = AmberWarm
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(AmberWarm.copy(alpha = 0.4f), Color(0x10FFFFFF))
                                )
                            )
                            .border(1.dp, AmberWarm.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank() || filterStatus != "All") "No Matching Invoices" else "Create Your First Invoice",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || filterStatus != "All")
                                "Try adjusting your search query or filter tags above."
                            else
                                "Generate professional, GST-compliant invoices in under 30 seconds with instant PDF export.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        )
                    }

                    if (invoices.isEmpty()) {
                        // Quick Feature Highlights
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x12FFFFFF))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = AmberWarm, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Automated GST Math (CGST, SGST & IGST)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.DocumentScanner, null, tint = SyncGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Smart OCR: Convert paper bills to invoices", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFF80EEFF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export to PDF, DOC & TXT in 1-tap", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Button(
                            onClick = onCreateInvoiceClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarm),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Invoice Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredInvoices.forEach { inv ->
                    InvoiceHistoryRow(
                        invoice = inv,
                        onEdit = { onEditInvoice(inv) },
                        onDelete = { onDeleteInvoice(inv) },
                        onDuplicate = { onDuplicateInvoice(inv) },
                        onShare = { onShareInvoice(inv, it) },
                        onPrint = { onPrintInvoice(inv) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        borderColorList = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
        ambientGlowColor = color
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subValue, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryRow(
    invoice: InvoiceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: (String) -> Unit,
    onPrint: () -> Unit
) {
    val customer = JsonHelper.fromCustomerJson(invoice.customerDetailsJson)
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Invoice ${invoice.invoiceNumber}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "To: ${customer.name}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onShare("PDF") }) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share", tint = AmberWarm)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Menu", tint = Color.White)
                        }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit / View") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Rounded.Add, null) },
                            onClick = { showMenu = false; onDuplicate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                            onClick = { showMenu = false; onShare("PDF") }
                        )
                        DropdownMenuItem(
                            text = { Text("Share TXT") },
                            leadingIcon = { Icon(Icons.Rounded.Description, null) },
                            onClick = { showMenu = false; onShare("TXT") }
                        )
                        DropdownMenuItem(
                            text = { Text("Share DOC") },
                            leadingIcon = { Icon(Icons.Rounded.Article, null) },
                            onClick = { showMenu = false; onShare("DOC") }
                        )
                        DropdownMenuItem(
                            text = { Text("Print Invoice") },
                            leadingIcon = { Icon(Icons.Rounded.Print, null) },
                            onClick = { showMenu = false; onPrint() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete permanently") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Color.Red) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date: ${invoice.invoiceDate}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                    Text(
                        text = "Source: ${invoice.source}",
                        style = MaterialTheme.typography.labelSmall.copy(color = if (invoice.source == "OCR") SyncGreen else WarmGold, fontSize = 10.sp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${invoice.currency} %.2f".format(invoice.grandTotal),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AmberWarm)
                    )
                    Text(
                        text = invoice.paymentStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = when (invoice.paymentStatus) {
                                "Paid" -> SyncGreen
                                "Unpaid" -> Color(0xFFFF5252)
                                else -> WarmGold
                            },
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: FORM BUILDER VIEW
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormBuilderScreen(
    sellerName: String, onSellerNameChange: (String) -> Unit,
    sellerOwner: String, onSellerOwnerChange: (String) -> Unit,
    sellerPhone: String, onSellerPhoneChange: (String) -> Unit,
    sellerEmail: String, onSellerEmailChange: (String) -> Unit,
    sellerAddress: String, onSellerAddressChange: (String) -> Unit,
    sellerCity: String, onSellerCityChange: (String) -> Unit,
    sellerState: String, onSellerStateChange: (String) -> Unit,
    sellerPinCode: String, onSellerPinCodeChange: (String) -> Unit,
    sellerGstin: String, onSellerGstinChange: (String) -> Unit,
    sellerPan: String, onSellerPanChange: (String) -> Unit,
    sellerWebsite: String, onSellerWebsiteChange: (String) -> Unit,
    sellerUpiId: String, onSellerUpiIdChange: (String) -> Unit,
    sellerBankName: String, onSellerBankNameChange: (String) -> Unit,
    sellerAccountNo: String, onSellerAccountNoChange: (String) -> Unit,
    sellerIfsc: String, onSellerIfscChange: (String) -> Unit,
    sellerNotes: String, onSellerNotesChange: (String) -> Unit,
    customerName: String, onCustomerNameChange: (String) -> Unit,
    customerCompany: String, onCustomerCompanyChange: (String) -> Unit,
    customerPhone: String, onCustomerPhoneChange: (String) -> Unit,
    customerEmail: String, onCustomerEmailChange: (String) -> Unit,
    customerBilling: String, onCustomerBillingChange: (String) -> Unit,
    customerShipping: String, onCustomerShippingChange: (String) -> Unit,
    customerGstin: String, onCustomerGstinChange: (String) -> Unit,
    customerPan: String, onCustomerPanChange: (String) -> Unit,
    customerNotes: String, onCustomerNotesChange: (String) -> Unit,
    invoiceNumber: String, onInvoiceNumberChange: (String) -> Unit,
    invoiceDate: String, onInvoiceDateChange: (String) -> Unit,
    dueDate: String, onDueDateChange: (String) -> Unit,
    paymentStatus: String, onPaymentStatusChange: (String) -> Unit,
    paymentMethod: String, onPaymentMethodChange: (String) -> Unit,
    referenceNo: String, onReferenceNoChange: (String) -> Unit,
    placeOfSupply: String, onPlaceOfSupplyChange: (String) -> Unit,
    reverseCharge: Boolean, onReverseChargeChange: (Boolean) -> Unit,
    notesField: String, onNotesFieldChange: (String) -> Unit,
    termsField: String, onTermsFieldChange: (String) -> Unit,
    selectedTemplate: String, onTemplateChange: (String) -> Unit,
    lineItems: MutableList<InvoiceItem>,
    subtotal: Double,
    totalDiscount: Double,
    taxableAmount: Double,
    cgst: Double,
    sgst: Double,
    igst: Double,
    cess: Double,
    roundOff: Double,
    grandTotal: Double,
    amountPaid: String,
    onAmountPaidChange: (String) -> Unit,
    balanceDue: Double,
    drawnSignaturePath: String?,
    signatureType: String,
    onSignatureDrawSave: (String) -> Unit,
    onSavedSellers: List<SellerProfileEntity>,
    onSavedCustomers: List<CustomerProfileEntity>,
    onSavedProducts: List<ProductEntity>,
    onSaveInvoice: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showSignaturePad by remember { mutableStateOf(false) }

    // Section expansion toggles
    var sellerExpanded by remember { mutableStateOf(false) }
    var customerExpanded by remember { mutableStateOf(false) }
    var itemsExpanded by remember { mutableStateOf(true) }
    var metaExpanded by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0x12FFFFFF),
        unfocusedContainerColor = Color(0x0CFFFFFF),
        focusedBorderColor = AmberWarm.copy(alpha = 0.6f),
        unfocusedBorderColor = GlassStroke,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = AmberWarm
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Business Profile Block
        ExpandableSectionCard(
            title = "Seller / Business Details",
            expanded = sellerExpanded,
            onToggle = { sellerExpanded = !sellerExpanded },
            color = AmberWarm
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Saved Profiles quick picker
                if (onSavedSellers.isNotEmpty()) {
                    Text("Pick Saved Profile:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        onSavedSellers.take(3).forEach { profile ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .clickable {
                                        onSellerNameChange(profile.name)
                                        onSellerOwnerChange(profile.ownerName)
                                        onSellerPhoneChange(profile.phone)
                                        onSellerEmailChange(profile.email)
                                        onSellerAddressChange(profile.address)
                                        onSellerCityChange(profile.city)
                                        onSellerStateChange(profile.state)
                                        onSellerPinCodeChange(profile.pinCode)
                                        onSellerGstinChange(profile.gstin)
                                        onSellerPanChange(profile.pan)
                                        onSellerWebsiteChange(profile.website)
                                        onSellerUpiIdChange(profile.upiId)
                                        onSellerBankNameChange(profile.bankName)
                                        onSellerAccountNoChange(profile.accountNumber)
                                        onSellerIfscChange(profile.ifsc)
                                        onSellerNotesChange(profile.additionalNotes)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(profile.name.take(15), color = AmberWarm, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = sellerName, onValueChange = onSellerNameChange,
                    placeholder = { Text("Enter seller/business name", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sellerOwner, onValueChange = onSellerOwnerChange,
                    placeholder = { Text("Owner / CEO Name", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sellerPhone, onValueChange = onSellerPhoneChange,
                        placeholder = { Text("Phone", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellerEmail, onValueChange = onSellerEmailChange,
                        placeholder = { Text("Email", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = sellerAddress, onValueChange = onSellerAddressChange,
                    placeholder = { Text("Address Line", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sellerCity, onValueChange = onSellerCityChange,
                        placeholder = { Text("City", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellerState, onValueChange = onSellerStateChange,
                        placeholder = { Text("State", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellerPinCode, onValueChange = onSellerPinCodeChange,
                        placeholder = { Text("PIN", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sellerGstin, onValueChange = onSellerGstinChange,
                        placeholder = { Text("GSTIN", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellerPan, onValueChange = onSellerPanChange,
                        placeholder = { Text("PAN", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                }

                // Bank & Payment Information
                Text("Bank & UPI (Payment details):", style = MaterialTheme.typography.labelSmall.copy(color = AmberWarm))

                OutlinedTextField(
                    value = sellerUpiId, onValueChange = onSellerUpiIdChange,
                    placeholder = { Text("UPI ID (For QR generation)", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sellerBankName, onValueChange = onSellerBankNameChange,
                    placeholder = { Text("Bank Name", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sellerAccountNo, onValueChange = onSellerAccountNoChange,
                        placeholder = { Text("Account Number", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = sellerIfsc, onValueChange = onSellerIfscChange,
                        placeholder = { Text("IFSC Code", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }

        // Customer Details Block
        ExpandableSectionCard(
            title = "Customer / Buyer Details",
            expanded = customerExpanded,
            onToggle = { customerExpanded = !customerExpanded },
            color = SyncGreen
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onSavedCustomers.isNotEmpty()) {
                    Text("Pick Saved Customer:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        onSavedCustomers.take(3).forEach { customer ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .clickable {
                                        onCustomerNameChange(customer.name)
                                        onCustomerCompanyChange(customer.companyName)
                                        onCustomerPhoneChange(customer.phone)
                                        onCustomerEmailChange(customer.email)
                                        onCustomerBillingChange(customer.billingAddress)
                                        onCustomerShippingChange(customer.shippingAddress)
                                        onCustomerGstinChange(customer.gstin)
                                        onCustomerPanChange(customer.pan)
                                        onCustomerNotesChange(customer.notes)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(customer.name.take(15), color = SyncGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = customerName, onValueChange = onCustomerNameChange,
                    placeholder = { Text("Enter buyer/client name", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerCompany, onValueChange = onCustomerCompanyChange,
                    placeholder = { Text("Company Name (Optional)", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customerPhone, onValueChange = onCustomerPhoneChange,
                        placeholder = { Text("Customer Phone", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customerEmail, onValueChange = onCustomerEmailChange,
                        placeholder = { Text("Customer Email", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = customerBilling, onValueChange = onCustomerBillingChange,
                    placeholder = { Text("Billing Address", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerShipping, onValueChange = onCustomerShippingChange,
                    placeholder = { Text("Shipping Address (Optional)", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customerGstin, onValueChange = onCustomerGstinChange,
                        placeholder = { Text("GSTIN", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customerPan, onValueChange = onCustomerPanChange,
                        placeholder = { Text("PAN", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Invoice Meta and dates
        ExpandableSectionCard(
            title = "Invoice Info & Dates",
            expanded = metaExpanded,
            onToggle = { metaExpanded = !metaExpanded },
            color = WarmGold
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = invoiceNumber, onValueChange = onInvoiceNumberChange,
                    placeholder = { Text("e.g., INV-001", color = TextMuted) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = invoiceDate, onValueChange = onInvoiceDateChange,
                        label = { Text("Issue Date (YYYY-MM-DD)", color = TextMuted, fontSize = 9.sp) },
                        placeholder = { Text("YYYY-MM-DD", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate, onValueChange = onDueDateChange,
                        label = { Text("Due Date (YYYY-MM-DD)", color = TextMuted, fontSize = 9.sp) },
                        placeholder = { Text("YYYY-MM-DD", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = referenceNo, onValueChange = onReferenceNoChange,
                        placeholder = { Text("PO / Reference No", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = placeOfSupply, onValueChange = onPlaceOfSupplyChange,
                        placeholder = { Text("Place of Supply", color = TextMuted) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reverse Charge", color = TextPrimary)
                    Switch(
                        checked = reverseCharge,
                        onCheckedChange = onReverseChargeChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = AmberWarm)
                    )
                }
            }
        }

        // Line Items Section Block
        ExpandableSectionCard(
            title = "Products & Services (Line Items)",
            expanded = itemsExpanded,
            onToggle = { itemsExpanded = !itemsExpanded },
            color = AmberWarm
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Line Items (${lineItems.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Quick Product Catalog Shortcut
                    if (onSavedProducts.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            onSavedProducts.take(2).forEach { prod ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AmberWarm.copy(alpha = 0.2f))
                                        .border(1.dp, AmberWarm.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val sub = 1.0 * prod.unitPrice
                                            val tax = sub * (prod.gstRate / 100.0)
                                            lineItems.add(
                                                InvoiceItem(
                                                    name = prod.name,
                                                    quantity = 1.0,
                                                    unitPrice = prod.unitPrice,
                                                    gstRatePercent = prod.gstRate,
                                                    cgstRatePercent = prod.gstRate / 2.0,
                                                    sgstRatePercent = prod.gstRate / 2.0,
                                                    cgstAmount = tax / 2.0,
                                                    sgstAmount = tax / 2.0,
                                                    lineTotal = sub + tax
                                                )
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+ ${prod.name.take(12)}",
                                        color = AmberWarm,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (lineItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x10FFFFFF))
                            .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddShoppingCart,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No items added yet.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap '+ Add Item / Service' below to begin.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    lineItems.forEachIndexed { index, item ->
                        InvoiceLineItemRow(
                            item = item,
                            index = index,
                            onItemChange = { updated ->
                                lineItems[index] = updated
                            },
                            onDelete = {
                                lineItems.removeAt(index)
                            },
                            textFieldColors = textFieldColors
                        )
                    }
                }

                // Prominent '+ Add Item' Button at bottom of item cards
                Button(
                    onClick = {
                        val defaultGst = 18.0
                        lineItems.add(
                            InvoiceItem(
                                name = "",
                                quantity = 1.0,
                                unitPrice = 0.0,
                                gstRatePercent = defaultGst,
                                cgstRatePercent = defaultGst / 2.0,
                                sgstRatePercent = defaultGst / 2.0,
                                lineTotal = 0.0
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x28FFB87E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarm.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Item",
                        tint = AmberWarm,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Add Item / Service",
                        color = AmberWarm,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }


        // Signature drawer launcher
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x18FFFFFF))
                .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                .clickable { showSignaturePad = true }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null, tint = AmberWarm)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (drawnSignaturePath == null) "Authorize & Draw Signature" else "Signature Captured ✓",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }

        // Templates & Custom Notes
        Text("Notes & Template Branding:", style = MaterialTheme.typography.labelSmall.copy(color = AmberWarm))

        // Selected Template Dropdown/Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Classic", "Modern", "Minimal", "GST-focused").forEach { template ->
                val isSelected = selectedTemplate == template
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AmberWarm.copy(alpha = 0.2f) else Color(0x10FFFFFF))
                        .border(1.dp, if (isSelected) AmberWarm else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { onTemplateChange(template) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(template, color = if (isSelected) AmberWarm else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = notesField, onValueChange = onNotesFieldChange,
            placeholder = { Text("Invoice Notes (e.g. Terms, UPI Payment, Thank You msg...)", color = TextMuted) },
            minLines = 3, shape = RoundedCornerShape(14.dp), colors = textFieldColors, modifier = Modifier.fillMaxWidth()
        )

        // Math totals summary card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            borderColorList = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Totals Summary (Accurate Math)", style = MaterialTheme.typography.labelSmall.copy(color = AmberWarm, fontWeight = FontWeight.Bold))
                
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Subtotal", color = TextSecondary)
                    Text("₹ %.2f".format(subtotal), color = TextPrimary)
                }
                if (totalDiscount > 0) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Discount", color = TextSecondary)
                        Text("-₹ %.2f".format(totalDiscount), color = TextPrimary)
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Taxable Value", color = TextSecondary)
                    Text("₹ %.2f".format(taxableAmount), color = TextPrimary)
                }
                if (cgst > 0) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("CGST (9%)", color = TextSecondary)
                        Text("₹ %.2f".format(cgst), color = TextPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("SGST (9%)", color = TextSecondary)
                        Text("₹ %.2f".format(sgst), color = TextPrimary)
                    }
                }
                if (igst > 0) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("IGST (18%)", color = TextSecondary)
                        Text("₹ %.2f".format(igst), color = TextPrimary)
                    }
                }

                // Balance inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount Paid (₹)", color = TextSecondary)
                    OutlinedTextField(
                        value = amountPaid,
                        onValueChange = onAmountPaidChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(120.dp)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 4.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Grand Total (Rounded)", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("₹ %.2f".format(grandTotal), color = AmberWarm, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Balance Due", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text("₹ %.2f".format(balanceDue), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SAVE & PERSIST INVOICE BUTTON
        Button(
            onClick = onSaveInvoice,
            colors = ButtonDefaults.buttonColors(containerColor = AmberWarm),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(50)
        ) {
            Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Invoice to History", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSave = { signaturePath ->
                onSignatureDrawSave(signaturePath)
                showSignaturePad = false
            }
        )
    }
}

// ==========================================
// SIMPLIFIED LINE ITEM ROW COMPONENT
// ==========================================

@Composable
fun InvoiceLineItemRow(
    item: InvoiceItem,
    index: Int,
    onItemChange: (InvoiceItem) -> Unit,
    onDelete: () -> Unit,
    textFieldColors: TextFieldColors
) {
    var qtyStr by remember(item.quantity) {
        mutableStateOf(
            if (item.quantity == item.quantity.toLong().toDouble() && item.quantity > 0)
                item.quantity.toLong().toString()
            else if (item.quantity == 0.0) ""
            else item.quantity.toString()
        )
    }
    var priceStr by remember(item.unitPrice) {
        mutableStateOf(
            if (item.unitPrice == item.unitPrice.toLong().toDouble() && item.unitPrice > 0)
                item.unitPrice.toLong().toString()
            else if (item.unitPrice == 0.0) ""
            else item.unitPrice.toString()
        )
    }
    var gstStr by remember(item.gstRatePercent) {
        mutableStateOf(
            if (item.gstRatePercent == item.gstRatePercent.toLong().toDouble())
                item.gstRatePercent.toLong().toString()
            else item.gstRatePercent.toString()
        )
    }

    val currentQty = qtyStr.toDoubleOrNull() ?: 0.0
    val currentPrice = priceStr.toDoubleOrNull() ?: 0.0
    val currentGst = gstStr.toDoubleOrNull() ?: 0.0

    val itemSubtotal = currentQty * currentPrice
    val itemTax = itemSubtotal * (currentGst / 100.0)
    val itemTotal = itemSubtotal + itemTax

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        borderColorList = listOf(Color(0x40FFFFFF), Color(0x15FFFFFF)),
        ambientGlowColor = AmberWarm
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Item / Service Name (Full width with delete trash icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { 
                        onItemChange(item.copy(name = it))
                    },
                    label = { Text("Item / Service Name", color = TextMuted, fontSize = 11.sp) },
                    placeholder = { Text("e.g. Website Design, Consulting, T-shirt...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x25FF5252))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Item",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Row 2: 3 clear labeled boxes in a neat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. [ Qty ]
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { input ->
                        qtyStr = input
                        val q = input.toDoubleOrNull() ?: 0.0
                        val sub = q * currentPrice
                        val tax = sub * (currentGst / 100.0)
                        onItemChange(
                            item.copy(
                                quantity = q,
                                cgstRatePercent = currentGst / 2.0,
                                sgstRatePercent = currentGst / 2.0,
                                cgstAmount = tax / 2.0,
                                sgstAmount = tax / 2.0,
                                lineTotal = sub + tax
                            )
                        )
                    },
                    label = { Text("Qty", color = TextMuted, fontSize = 11.sp) },
                    placeholder = { Text("1", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )

                // 2. [ Rate / Price (₹) ]
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { input ->
                        priceStr = input
                        val p = input.toDoubleOrNull() ?: 0.0
                        val sub = currentQty * p
                        val tax = sub * (currentGst / 100.0)
                        onItemChange(
                            item.copy(
                                unitPrice = p,
                                cgstRatePercent = currentGst / 2.0,
                                sgstRatePercent = currentGst / 2.0,
                                cgstAmount = tax / 2.0,
                                sgstAmount = tax / 2.0,
                                lineTotal = sub + tax
                            )
                        )
                    },
                    label = { Text("Rate (₹)", color = TextMuted, fontSize = 11.sp) },
                    placeholder = { Text("500", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.weight(1.3f)
                )

                // 3. [ Tax / GST % ]
                OutlinedTextField(
                    value = gstStr,
                    onValueChange = { input ->
                        gstStr = input
                        val g = input.toDoubleOrNull() ?: 0.0
                        val sub = currentQty * currentPrice
                        val tax = sub * (g / 100.0)
                        onItemChange(
                            item.copy(
                                gstRatePercent = g,
                                cgstRatePercent = g / 2.0,
                                sgstRatePercent = g / 2.0,
                                cgstAmount = tax / 2.0,
                                sgstAmount = tax / 2.0,
                                lineTotal = sub + tax
                            )
                        )
                    },
                    label = { Text("GST %", color = TextMuted, fontSize = 11.sp) },
                    placeholder = { Text("18%", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.weight(1.1f)
                )
            }

            // Row 3: Live calculated item total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Base: ₹%.2f + GST: ₹%.2f".format(itemSubtotal, itemTax),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Item Total: ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "₹ %.2f".format(itemTotal),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = AmberWarm,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// SIGNATURE PAD COMPONENT
// ==========================================

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val path = remember { Path() }
    val pathsList = remember { mutableStateListOf<Path>() }
    var currentOffset by remember { mutableStateOf<Offset?>(null) }

    // Double-buffered drawing bitmap to export high-res PNG file offline
    val sigBitmap = remember { Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888) }
    val sigCanvas = remember { Canvas(sigBitmap) }
    val sigPaint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 6f
            style = Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    LaunchedEffect(Unit) {
        sigCanvas.drawColor(android.graphics.Color.WHITE)
    }

    FrostedGlassAlertDialog(
        onDismissRequest = onDismiss,
        title = "Draw Authorized Signature",
        confirmButtonText = "Save Signature",
        onConfirm = {
            try {
                val imagesDir = File(context.filesDir, "vault_signatures").apply { mkdirs() }
                val sigFile = File(imagesDir, "signature_${System.currentTimeMillis()}.png")
                FileOutputStream(sigFile).use { out ->
                    sigBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                onSave(sigFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        dismissButtonText = "Cancel",
        onDismiss = onDismiss,
        icon = Icons.Rounded.Edit,
        iconTint = AmberWarm,
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    path.moveTo(offset.x, offset.y)
                                    currentOffset = offset
                                },
                                onDrag = { change, dragAmount ->
                                    val newPos = change.position
                                    path.lineTo(newPos.x, newPos.y)
                                    
                                    // Draw onto persistent bitmap
                                    if (currentOffset != null) {
                                        sigCanvas.drawLine(currentOffset!!.x, currentOffset!!.y, newPos.x, newPos.y, sigPaint)
                                    }
                                    currentOffset = newPos
                                    
                                    // Re-trigger compose redraw
                                    if (!pathsList.contains(path)) {
                                        pathsList.add(path)
                                    }
                                },
                                onDragEnd = {
                                    currentOffset = null
                                }
                            )
                        }
                ) {
                    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                        drawPath(path = path, color = Color.Black, style = Stroke(width = 5f))
                    }
                }
                Text("Draw signature on pad using finger", color = TextSecondary, fontSize = 11.sp)
            }
        }
    )
}

// ==========================================
// SCREEN 2: OCR REVIEW SCREEN
// ==========================================

@Composable
fun OCRReviewScreen(
    parsedInvoice: ParsedOcrInvoice,
    onAccept: (ParsedOcrInvoice) -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberScrollState()

    var sellerName by remember { mutableStateOf(parsedInvoice.seller.name) }
    var sellerGstin by remember { mutableStateOf(parsedInvoice.seller.gstin) }
    var customerName by remember { mutableStateOf(parsedInvoice.customer.name) }
    var invoiceNo by remember { mutableStateOf(parsedInvoice.invoiceNumber) }
    var invoiceDate by remember { mutableStateOf(parsedInvoice.invoiceDate) }
    var scannedTotalVal by remember { mutableStateOf(parsedInvoice.scannedTotal.toString()) }

    val reviewedItems = remember { mutableStateListOf<InvoiceItem>().apply { addAll(parsedInvoice.items) } }

    val calcSubtotal = reviewedItems.sumOf { it.quantity * it.unitPrice }
    val calcTax = reviewedItems.sumOf { it.cgstAmount + it.sgstAmount + it.igstAmount }
    val calcGrandTotal = calcSubtotal + calcTax

    val parsedScannedDouble = scannedTotalVal.toDoubleOrNull() ?: 0.0
    val mismatch = Math.abs(parsedScannedDouble - calcGrandTotal) > 2.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Validation Warning banner
        if (mismatch) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x24FF5252))
                    .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF5252))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Totals Mismatch Warning!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Scanned total (₹ %.2f) does not match calculated total (₹ %.2f). Please check rate values.".format(parsedScannedDouble, calcGrandTotal), color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x2400E5FF))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Calculated totals match the scanned bill perfectly! confidence: ${parsedInvoice.confidenceScore}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Editable Review fields
        Text("Verify Extracted Party Info:", style = MaterialTheme.typography.labelSmall.copy(color = AmberWarm))

        OutlinedTextField(
            value = sellerName, onValueChange = { sellerName = it },
            label = { Text("Seller / Vendor Name") }, modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = sellerGstin, onValueChange = { sellerGstin = it },
            label = { Text("Seller GSTIN") }, modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = customerName, onValueChange = { customerName = it },
            label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = invoiceNo, onValueChange = { invoiceNo = it },
                label = { Text("Invoice No") }, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = invoiceDate, onValueChange = { invoiceDate = it },
                label = { Text("Invoice Date") }, modifier = Modifier.weight(1f)
            )
        }

        Text("Reconstructed Table Items:", style = MaterialTheme.typography.labelSmall.copy(color = AmberWarm))

        reviewedItems.forEachIndexed { idx, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x10FFFFFF))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.name, onValueChange = { reviewedItems[idx] = item.copy(name = it) },
                    placeholder = { Text("Item Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = item.quantity.toString(), onValueChange = {
                            val q = it.toDoubleOrNull() ?: 1.0
                            val sub = q * item.unitPrice
                            reviewedItems[idx] = item.copy(quantity = q, lineTotal = sub + (sub * 0.18))
                        },
                        label = { Text("Qty") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = item.unitPrice.toString(), onValueChange = {
                            val r = it.toDoubleOrNull() ?: 0.0
                            val sub = item.quantity * r
                            reviewedItems[idx] = item.copy(unitPrice = r, lineTotal = sub + (sub * 0.18))
                        },
                        label = { Text("Rate") }, modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x20FFFFFF)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val finalReviewed = ParsedOcrInvoice(
                        seller = parsedInvoice.seller.copy(name = sellerName, gstin = sellerGstin),
                        customer = parsedInvoice.customer.copy(name = customerName),
                        invoiceNumber = invoiceNo,
                        invoiceDate = invoiceDate,
                        dueDate = invoiceDate,
                        items = reviewedItems.toList(),
                        scannedTotal = parsedScannedDouble,
                        calculatedTotal = calcGrandTotal,
                        totalsMismatch = mismatch,
                        confidenceScore = parsedInvoice.confidenceScore,
                        uncertainFields = parsedInvoice.uncertainFields,
                        isOcrUncertain = mismatch
                    )
                    onAccept(finalReviewed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm),
                modifier = Modifier.weight(1.5f)
            ) {
                Text("Generate Digital Invoice", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// SCREEN 3: SAVED PROFILES SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedProfilesScreen(
    sellerProfiles: List<SellerProfileEntity>,
    customerProfiles: List<CustomerProfileEntity>,
    productsList: List<ProductEntity>,
    onSaveSeller: (SellerProfileEntity) -> Unit,
    onDeleteSeller: (SellerProfileEntity) -> Unit,
    onSaveCustomer: (CustomerProfileEntity) -> Unit,
    onDeleteCustomer: (CustomerProfileEntity) -> Unit,
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Sellers, 1 = Customers, 2 = Products
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = AmberWarm
                )
            }
        ) {
            listOf("Businesses", "Customers", "Products").forEachIndexed { idx, title ->
                Tab(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    text = { Text(title, color = if (activeTab == idx) AmberWarm else TextSecondary, fontWeight = FontWeight.Bold) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (activeTab) {
                0 -> {
                    if (sellerProfiles.isEmpty()) {
                        item {
                            Text("No saved business profiles yet.", color = TextMuted, modifier = Modifier.padding(20.dp))
                        }
                    } else {
                        items(sellerProfiles) { seller ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(seller.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("GST: ${seller.gstin}", color = TextSecondary, fontSize = 11.sp)
                                        Text("UPI ID: ${seller.upiId}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { onDeleteSeller(seller) }) {
                                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (customerProfiles.isEmpty()) {
                        item {
                            Text("No saved customers yet.", color = TextMuted, modifier = Modifier.padding(20.dp))
                        }
                    } else {
                        items(customerProfiles) { customer ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(customer.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("GST: ${customer.gstin}", color = TextSecondary, fontSize = 11.sp)
                                        Text("Email: ${customer.email}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { onDeleteCustomer(customer) }) {
                                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (productsList.isEmpty()) {
                        item {
                            Text("No saved products or services yet.", color = TextMuted, modifier = Modifier.padding(20.dp))
                        }
                    } else {
                        items(productsList) { prod ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(prod.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Rate: ₹ ${String.format("%.2f", prod.unitPrice)}", color = TextSecondary, fontSize = 11.sp)
                                        Text("GST: ${prod.gstRate}%", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { onDeleteProduct(prod) }) {
                                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CUSTOM UTILITY COMPOSABLES
// ==========================================

@Composable
fun ExpandableSectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    color: Color,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = color
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}
