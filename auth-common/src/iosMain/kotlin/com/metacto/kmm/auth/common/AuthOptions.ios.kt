package com.metacto.kmm.auth.common

import platform.AuthenticationServices.ASPresentationAnchor
import platform.UIKit.UIViewController

actual class AuthOptions(
    val presentingViewController: UIViewController,
    val presentationAnchor: ASPresentationAnchor
)