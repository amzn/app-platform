//
//  PresenterModelView.swift
//  recipesIosApp
//
//  Created by Wang, Jessalyn on 11/24/25.
//

import SwiftUI

/// A generic view that renders `Models` conforming to `SelfRenderingViewModel`.
///
/// This view is a lightweight wrapper that delegates view creation to the `Models` themselves. `Models` must conform
/// to `SelfRenderingViewModel` and provide their view in `makeViewRenderer()`. This is a simple API that
/// enables view creation for models regardless of type. If needed this implementation can be changed to follow a factory pattern.
struct PresenterModelView<Model>: View {
    var model: Model

    init(model: Model) {
        self.model = model
    }

    var body: some View {
        let type = type(of: model as Any)

        if let selfRenderingModel = model as? (any SelfRenderingViewModel) {
            return AnyView(selfRenderingModel.makeViewRenderer())
        }

        fatalError("Could not find view builder for \(type). Make \(type) conform to `SelfRenderingViewModel` protocol.")
    }
}
