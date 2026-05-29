package com.crescenzi.remagic.presentation.auth

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.crescenzi.remagic.R
import com.crescenzi.remagic.core.LOG
import com.crescenzi.remagic.external.firebase.NativeFirebase

@Composable
fun AuthScreen(
    onAuth: (email: String, pwd: String, action: NativeFirebase.AuthAction) -> Unit
) {

    val resources = LocalResources.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }

    val bgPainter = remember {
        BitmapPainter(
            BitmapFactory.decodeResource(resources, R.raw.game_bg).asImageBitmap()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = bgPainter,
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(28.dp)
                .background(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    RoundedCornerShape(20.dp)
                )
                .padding(28.dp)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

//            Text(
//                text = if (isLogin) stringResource(R.string.login_title) else stringResource(R.string.register_title),
//                style = MaterialTheme.typography.titleMedium,
//                color = MaterialTheme.colorScheme.onBackground
//            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                singleLine = true,
                label = { Text(stringResource(R.string.email_label), style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.fillMaxWidth().scale(0.8f),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(stringResource(R.string.password_label), style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.fillMaxWidth().scale(0.8f)
            )

            Button(
                onClick = {
                    if(email.isNotEmpty() && password.isNotEmpty()){
                        onAuth(
                            email,
                            password,
                            if (isLogin)
                                NativeFirebase.AuthAction.SIGN_IN
                            else
                                NativeFirebase.AuthAction.SIGN_UP
                        )
                    }else LOG("Invalid UI values (empty) ")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp).scale(0.8f)
            ) {
                Text(
                    text = if (isLogin) stringResource(R.string.enter_button) else stringResource(R.string.create_button),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    text = if (isLogin)
                        stringResource(R.string.switch_to_register)
                    else
                        stringResource(R.string.switch_to_login),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Text(
                text = stringResource(R.string.auth_beta_notice),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
