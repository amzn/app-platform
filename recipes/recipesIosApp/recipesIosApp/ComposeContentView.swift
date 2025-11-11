//
//  ComposeContentView.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/10/25.
//

import SwiftUI
import RecipesApp

struct ComposeView: UIViewControllerRepresentable {
    private var rootScopeProvider: RootScopeProvider

    init(rootScopeProvider: RootScopeProvider) {
        self.rootScopeProvider = rootScopeProvider
    }

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(rootScopeProvider: rootScopeProvider)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ComposeContentView: View {
    var rootScopeProvider: RootScopeProvider

    init(rootScopeProvider: RootScopeProvider) {
        self.rootScopeProvider = rootScopeProvider
    }

    var body: some View {
        ComposeView(rootScopeProvider: rootScopeProvider).ignoresSafeArea(.keyboard) // Compose has its own keyboard handler
    }
}
