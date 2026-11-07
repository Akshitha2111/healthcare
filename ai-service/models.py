"""Model definitions shared by training and inference.

Scikit-learn candidates are plain estimators. The Keras network is wrapped in a
small sklearn-style class so that cross-validation, evaluation and inference
code is identical for every model. TensorFlow is imported lazily, so the
service starts quickly (and does not need TensorFlow at all) when a
scikit-learn model is deployed.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from sklearn.base import BaseEstimator, ClassifierMixin
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.svm import SVC
from sklearn.tree import DecisionTreeClassifier


class KerasMLP(BaseEstimator, ClassifierMixin):
    """Small fully connected network on binary symptom indicators.

    Hyper-parameters are fixed a priori (no tuning) because the dataset has
    only a few hundred unique records; see docs/architecture.md.
    """

    def __init__(self, epochs: int = 120, batch_size: int = 16, seed: int = 42,
                 hidden: tuple = (128, 64), dropout: float = 0.3, l2: float = 1e-4):
        self.epochs = epochs
        self.batch_size = batch_size
        self.seed = seed
        self.hidden = hidden
        self.dropout = dropout
        self.l2 = l2

    def _build(self, n_features: int, n_classes: int):
        import tensorflow as tf
        from tensorflow import keras
        from tensorflow.keras import layers, regularizers

        tf.keras.utils.set_random_seed(self.seed)
        reg = regularizers.l2(self.l2)
        model = keras.Sequential([layers.Input(shape=(n_features,))])
        for units in self.hidden:
            model.add(layers.Dense(units, activation="relu", kernel_regularizer=reg))
            model.add(layers.Dropout(self.dropout))
        model.add(layers.Dense(n_classes, activation="softmax"))
        model.compile(optimizer=keras.optimizers.Adam(1e-3),
                      loss="sparse_categorical_crossentropy", metrics=["accuracy"])
        return model

    def fit(self, X, y):
        X = np.asarray(X, dtype=np.float32)
        self.classes_, y_idx = np.unique(np.asarray(y), return_inverse=True)
        self.model_ = self._build(X.shape[1], len(self.classes_))
        self.model_.fit(X, y_idx, epochs=self.epochs, batch_size=self.batch_size,
                        verbose=0, shuffle=True)
        return self

    def predict_proba(self, X):
        X = np.asarray(X, dtype=np.float32)
        return self.model_.predict(X, verbose=0)

    def predict(self, X):
        return self.classes_[np.argmax(self.predict_proba(X), axis=1)]

    # --- persistence -------------------------------------------------
    def save(self, path: Path) -> None:
        self.model_.save(str(path))

    @classmethod
    def load(cls, path: Path, classes: list[str]) -> "KerasMLP":
        from tensorflow import keras

        obj = cls()
        obj.model_ = keras.models.load_model(str(path))
        obj.classes_ = np.asarray(classes)
        return obj


def build_candidates(seed: int) -> dict[str, object]:
    """Candidate classifiers, in order of increasing complexity (used as the
    tie-breaker when selecting a model)."""
    return {
        "logistic_regression": LogisticRegression(max_iter=3000, C=10.0, random_state=seed),
        "decision_tree": DecisionTreeClassifier(random_state=seed),
        "random_forest": RandomForestClassifier(n_estimators=300, random_state=seed, n_jobs=1),
        "svm_linear": SVC(kernel="linear", probability=True, random_state=seed),
        "keras_mlp": KerasMLP(seed=seed),
    }
