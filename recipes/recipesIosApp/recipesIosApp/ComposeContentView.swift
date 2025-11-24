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
    
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let composeVC = MainViewControllerKt.mainViewController(rootScopeProvider: rootScopeProvider) { model in
            context.coordinator.navigateToNativeViewController(model: model)
        }
                
        // Wrap in navigation controller
        let navController = UINavigationController(rootViewController: composeVC)
        
        context.coordinator.navigationController = navController
        
        return navController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    
    class Coordinator {
        weak var navigationController: UINavigationController?
        
        func navigateToNativeViewController(model: BaseModel) {
            let modelHash = ObjectIdentifier(model as AnyObject).hashValue
            
            DispatchQueue.main.async { [weak self] in
                guard let navController = self?.navigationController else { return }
                
                let detailVC = CounterViewController()
                navController.pushViewController(detailVC, animated: true)
            }
        }
    }
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
