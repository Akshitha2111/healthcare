"""Small TensorFlow/Keras MLP with a scikit-learn-like interface.

TensorFlow is imported lazily so the Flask service (and the tests) work without it unless
the Keras model is the one selected for serving.
"""
from __future__ import annotations

import os

os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import numpy as np


class KerasMLPClassifier:
    """Dense(64) -> Dropout -> Dense(32) -> Dropout -> softmax, with L2 regularisation.

    Sized for ~250 binary features/samples: a larger network would simply memorise the data.
    """

    def __init__(self, seed: int = 42, epochs: int = 200, batch_size: int = 16,
                 dropout: float = 0.3, l2: float = 1e-4, learning_rate: float = 1e-3):
        self.seed = seed
        self.epochs = epochs
        self.batch_size = batch_size
        self.dropout = dropout
        self.l2 = l2
        self.learning_rate = learning_rate
        self.classes_ = None
        self.model_ = None

    def _build(self, n_features: int, n_classes: int):
        import tensorflow as tf

        tf.keras.utils.set_random_seed(self.seed)
        reg = tf.keras.regularizers.l2(self.l2)
        model = tf.keras.Sequential([
            tf.keras.layers.Input(shape=(n_features,)),
            tf.keras.layers.Dense(64, activation="relu", kernel_regularizer=reg),
            tf.keras.layers.Dropout(self.dropout),
            tf.keras.layers.Dense(32, activation="relu", kernel_regularizer=reg),
            tf.keras.layers.Dropout(self.dropout),
            tf.keras.layers.Dense(n_classes, activation="softmax"),
        ])
        model.compile(
            optimizer=tf.keras.optimizers.Adam(self.learning_rate),
            loss="sparse_categorical_crossentropy",
            metrics=["accuracy"],
        )
        return model

    def fit(self, X, y):
        y = np.asarray(y, dtype=str)
        self.classes_ = np.array(sorted(set(y.tolist())))
        y_idx = np.searchsorted(self.classes_, y)
        self.model_ = self._build(X.shape[1], len(self.classes_))
        self.model_.fit(np.asarray(X, dtype="float32"), y_idx, epochs=self.epochs,
                        batch_size=self.batch_size, verbose=0, shuffle=True)
        return self

    def predict_proba(self, X) -> np.ndarray:
        return self.model_(np.asarray(X, dtype="float32"), training=False).numpy()

    def predict(self, X) -> np.ndarray:
        return self.classes_[np.argmax(self.predict_proba(X), axis=1)]

    def save(self, path) -> None:
        self.model_.save(str(path))

    @classmethod
    def load(cls, path, classes) -> "KerasMLPClassifier":
        import tensorflow as tf

        obj = cls()
        obj.model_ = tf.keras.models.load_model(str(path))
        obj.classes_ = np.array(classes)
        return obj
