FROM payara/server-node:5.2021.2

COPY --chown=payara:payara bin/* ${SCRIPT_DIR}

RUN true \
    && chmod +x ${SCRIPT_DIR}/entrypoint.sh \
    && true

# entrypoint.sh suited for Kubernetes.
ENTRYPOINT "${SCRIPT_DIR}/entrypoint.sh"
