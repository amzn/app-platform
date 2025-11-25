//
//  SelfRenderingViewModel.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import SwiftUI

/// A protocol for view models that create their own SwiftUI view representation.
protocol SelfRenderingViewModel {
    associatedtype Renderer : View
    @ViewBuilder @MainActor func makeViewRenderer() -> Self.Renderer
}
