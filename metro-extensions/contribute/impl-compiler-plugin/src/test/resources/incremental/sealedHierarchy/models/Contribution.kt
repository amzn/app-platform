package com.test

import software.ralf.app.platform.presenter.BaseModel

sealed interface Model : BaseModel {
  sealed interface Branch : Model {
    data object First : Branch
  }
}
