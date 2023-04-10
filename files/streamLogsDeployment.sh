#!/bin/bash

# Replace <deployment-name> with the name of your Deployment
DEPLOYMENT_NAME="reciter-pm-dev"
# Replace <namespace> with the name of your namespace
NAMESPACE="reciter"

# Function to stream logs for a pod
stream_logs() {
  POD_NAME=$1
  echo "Streaming logs for pod: ${POD_NAME}"
  kubectl -n "${NAMESPACE}" logs -f --since=10s ${POD_NAME} &
}

# Get current pods for the Deployment based on name pattern
CURRENT_PODS=$(kubectl -n "${NAMESPACE}" get pods -o json | jq -r ".items[] | select(.metadata.name | startswith(\"${DEPLOYMENT_NAME}\")) | .metadata.name")

# Stream logs for current pods
for POD_NAME in ${CURRENT_PODS}; do
  stream_logs ${POD_NAME}
done

# Continuously watch for new pods and stream logs
kubectl -n "${NAMESPACE}" get pods --watch-only -o json | jq --unbuffered -r "select(.type==\"ADDED\") | select(.object.metadata.name | startswith(\"${DEPLOYMENT_NAME}\")) | .object.metadata.name" | while read NEW_POD; do
  stream_logs ${NEW_POD}
done
