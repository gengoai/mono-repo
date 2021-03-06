from collections import defaultdict
from typing import DefaultDict

import keras as K
import numpy as np
import tensorflow as tf
import tensorflow_hub as hub


class ElmoEmbeddingLayer(K.layers.Layer):
    def __init__(self, trainable=True, **kwargs):
        self.dimensions = 1024
        self.trainable = trainable
        super(ElmoEmbeddingLayer, self).__init__(**kwargs)

    def build(self, input_shape):
        self.elmo = hub.Module('https://tfhub.dev/google/elmo/2',
                               trainable=self.trainable,
                               name="{}_module".format(self.name))
        self.trainable_weights += K.backend.tf.trainable_variables(scope="^{}_module/.*".format(self.name))
        super(ElmoEmbeddingLayer, self).build(input_shape[0])

    def call(self, x, mask=None):
        sequence_len = K.backend.reshape(K.backend.cast(x[1], tf.int32), shape=(-1,))
        result = self.elmo(inputs={"tokens": x[0],
                                   "sequence_len": sequence_len},
                           as_dict=True,
                           signature='tokens')['elmo']
        return result

    def compute_mask(self, inputs, mask=None):
        return K.backend.not_equal(inputs[0], '--PAD--')

    def compute_output_shape(self, input_shape):
        return (input_shape[0][0], input_shape[0][1], self.dimensions)


class GloveEmbedding(K.layers.Embedding):
    __glove_weights: DefaultDict[str, np.ndarray] = defaultdict(lambda: None)

    @classmethod
    def __get_weights(cls, dimension: int, path: str) -> np.ndarray:
        if not cls.__glove_weights[dimension]:
            cls.__glove_weights[dimension] = np.load(path % dimension)
        return cls.__glove_weights[dimension]

    def __init__(self,
                 dimension: int,
                 glove_path: str,
                 mask_zero: bool = False):
        weights = GloveEmbedding.__get_weights(dimension, glove_path)
        super(GloveEmbedding, self).__init__(input_dim=weights.shape[0],
                                             output_dim=weights.shape[1],
                                             weights=[weights],
                                             trainable=False,
                                             mask_zero=mask_zero,
                                             name="glove%s_embeddings" % dimension)


class MeanPool(K.layers.Layer):
    def __init__(self, **kwargs):
        self.supports_masking = True
        super(MeanPool, self).__init__(**kwargs)

    def compute_mask(self, input, input_mask=None):
        # do not pass the mask to the next layers
        return None

    def call(self, x, mask=None):
        if mask is not None:
            # mask (batch, time)
            mask = K.backend.cast(mask, K.backend.floatx())
            # mask (batch, x_dim, time)
            mask = K.backend.repeat(mask, x.shape[-1])
            # mask (batch, time, x_dim)
            mask = tf.transpose(mask, [0, 2, 1])
            x = x * mask
            return K.backend.sum(x, axis=1) / K.backend.sum(mask, axis=1)
        return K.backend.mean(x, axis=1)

    def compute_output_shape(self, input_shape):
        # remove temporal dimension
        return (input_shape[0], input_shape[2])
