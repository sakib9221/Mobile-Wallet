package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.GoogleAuthDialog
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class FinanceDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testGoogleAvatarRendering() {
        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.GoogleAvatar(email = "test@gmail.com")
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testGoogleAuthDialogFlow() {
        var signedInEmail: String? = null
        var dismissed = false

        composeTestRule.setContent {
            MyApplicationTheme {
                GoogleAuthDialog(
                    currentUser = null,
                    selectedTheme = "system",
                    onThemeChange = {},
                    onDismiss = { dismissed = true },
                    onSignInSuccess = { email -> signedInEmail = email },
                    onSignOut = {},
                    getString = { "Test String" }
                )
            }
        }

        // Test display input exists
        composeTestRule.onNodeWithTag("google_email_input").assertExists()
        
        // Enter email and submit
        composeTestRule.onNodeWithTag("google_email_input").performTextInput("user@gmail.com")
        composeTestRule.onNodeWithTag("google_signin_submit").performClick()
        
        // Run coroutines/delay for simulated login verifying account state
        composeTestRule.waitForIdle()
        
        // Let LaunchedEffect trigger
        composeTestRule.mainClock.advanceTimeBy(1500)
        
        // Verify signedInEmail was set
        assertEquals("user@gmail.com", signedInEmail)
    }

    @Test
    fun testDeveloperCreditDialogRendering() {
        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.DeveloperCreditDialog(
                    currentUser = "test@gmail.com",
                    onDismiss = {}
                )
            }
        }
        // Surface tag matches .testTag("developer_credit_dialog_surface")
        composeTestRule.onNodeWithTag("developer_credit_dialog_surface").assertExists()
        composeTestRule.onNodeWithText("Developer Credit").assertExists()
    }

    @Test
    fun testDeveloperContactDialogRendering() {
        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.DeveloperContactDialog(
                    onDismiss = {}
                )
            }
        }
        // Surface tag matches .testTag("developer_contact_dialog_surface")
        composeTestRule.onNodeWithTag("developer_contact_dialog_surface").assertExists()
        composeTestRule.onNodeWithText("Telegram").assertExists()
        composeTestRule.onNodeWithText("@sakib_9221").assertExists()
        composeTestRule.onNodeWithText("WhatsApp").assertExists()
        composeTestRule.onNodeWithText("Facebook").assertExists()
    }
}
